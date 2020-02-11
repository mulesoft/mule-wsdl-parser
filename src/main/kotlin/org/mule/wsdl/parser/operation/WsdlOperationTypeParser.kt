package org.mule.wsdl.parser.operation

import com.ibm.wsdl.BindingInputImpl
import com.ibm.wsdl.BindingOutputImpl
import com.ibm.wsdl.MessageImpl
import org.mule.metadata.api.TypeLoader
import org.mule.metadata.api.builder.BaseTypeBuilder
import org.mule.metadata.api.model.BinaryType
import org.mule.metadata.api.model.MetadataFormat.JAVA
import org.mule.metadata.api.model.MetadataFormat.XML
import org.mule.metadata.api.model.MetadataType
import org.mule.metadata.api.model.ObjectFieldType
import org.mule.metadata.api.model.ObjectType
import org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart
import org.mule.wsdl.parser.model.message.MessageDefinition
import org.mule.wsdl.parser.model.operation.SoapHeader
import org.mule.wsdl.parser.model.operation.Type
import org.slf4j.LoggerFactory
import java.util.Optional
import javax.wsdl.BindingOperation
import javax.wsdl.Definition
import javax.wsdl.Message
import javax.wsdl.Part
import javax.wsdl.extensions.ElementExtensible
import javax.wsdl.extensions.mime.MIMEMultipartRelated
import javax.wsdl.extensions.soap.SOAPBody
import javax.wsdl.extensions.soap.SOAPHeader
import javax.wsdl.extensions.soap12.SOAP12Body
import javax.wsdl.extensions.soap12.SOAP12Header

class WsdlOperationTypeParser private constructor(private val wsdl: Definition,
                                                  private val loader: TypeLoader,
                                                  private val name: String,
                                                  private val bindingType: ElementExtensible,
                                                  private val message: Message) {

  companion object {
    private val LOGGER = LoggerFactory.getLogger(WsdlOperationTypeParser.javaClass)

    fun parseInput(wsdl: Definition, loader: TypeLoader, bop: BindingOperation): Type =
      WsdlOperationTypeParser(wsdl, loader, bop.name, bop.bindingInput ?: BindingInputImpl(), bop.operation.input?.message ?: MessageImpl()).parse()

    fun parseOutput(wsdl: Definition, loader: TypeLoader, bop: BindingOperation): Type =
      WsdlOperationTypeParser(wsdl, loader, bop.name, bop.bindingOutput ?: BindingOutputImpl(), bop.operation.output?.message ?: MessageImpl()).parse()

    val NULL_TYPE = BaseTypeBuilder.create(XML).nullType().build()
    val ANY_TYPE = BaseTypeBuilder.create(JAVA).anyType().build()
  }

  private fun parse(): Type {
    return Type(buildOrGetDefaultType(this::buildBody), buildOrGetDefaultType(this::buildHeaders), buildOrGetDefaultType(this::buildAttachments), MessageDefinition.fromMessage(message, bindingType))
  }

  private fun buildBody(): MetadataType {
    val bodyPart = getBodyPart()
    return if (bodyPart != null) {
      val bodyType = buildPartMetadataType(bodyPart)
      val attachmentFields = getAttachmentFields(bodyType)
      filterAttachmentsFromBodyType(bodyType, attachmentFields)
    } else {
      // using document style, a message declaration with no parts (either input or output) will generate an empty SOAP body.
      NULL_TYPE
    }
  }

  /**
   * Filter the attachments fields from the body metadata type since SOAP manages the attachments as regular parameters but
   * we wan't to provide a body decoupled experience for the attachments.
   *
   *
   * If after removing the attachments there are not fields remaining in the request, a [NullType] is returned.
   *
   * @param bodyType    the [MetadataType] of the xml input body, with all the required parameters including the
   * @param attachments the attachments fields on found in the type.
   * @return the body [MetadataType] without the attachment fields.
   */
  private fun filterAttachmentsFromBodyType(bodyType: MetadataType, attachments: List<ObjectFieldType>): MetadataType {
    if (!attachments.isEmpty() && bodyType is ObjectType) {
      val operationType = getOperationType(bodyType)
      if (operationType is ObjectType) {
        attachments.forEach { a -> operationType.fields.removeIf { f -> getLocalPart(f) == getLocalPart(a) } }
        if (operationType.fields.isEmpty()) {
          return NULL_TYPE
        }
      }
    }
    return bodyType
  }

  private fun buildHeaders(): MetadataType {
    val headerParts = getHeaderParts(bindingType)
    if (headerParts.isEmpty()) {
      return NULL_TYPE
    }
    val typeBuilder = BaseTypeBuilder.create(XML)
    val headersXml = typeBuilder.objectType()
    val headers = headersXml.addField().key("headers").value().objectType()
    for (header in headerParts) {
      val field = headers.addField()
      val headerPart = header.partName
      val part = message?.getPart(headerPart)
      if (part != null) {
        field.key(headerPart).value(buildPartMetadataType(part))
      } else {
        val headerMessage = wsdl.getMessage(header.qName)
        field.key(headerPart).value(buildPartMetadataType(headerMessage.getPart(headerPart)))
      }
    }
    return headersXml.build()
  }


  private fun buildPartMetadataType(part: Part): MetadataType {
    if (part.elementName != null) {
      val partName = part.elementName.toString()
      return loader.load(partName).orElseThrow { RuntimeException("Could not load part element name [$partName]") }
    }
    throw RuntimeException("Trying to resolve metadata for a nameless part, probably the provided WSDL is invalid")
  }

  private fun getBodyPart(): Part? {
    val parts = message?.parts
    if (parts != null && !parts.isEmpty()) {
      if (parts.size == 1) {
        return parts[parts.keys.toTypedArray()[0]] as Part
      }
      val part = getBodyPartName(bindingType).flatMap<Part> { partName -> Optional.ofNullable(parts[partName] as Part) }
      if (part.isPresent) return part.get()
    }
    return null
  }

  private fun getBodyPartName(bindingType: ElementExtensible): Optional<String> {
    val elements = bindingType.extensibilityElements
    if (elements != null) {
      val bodyParts = getPartNames(bindingType)
      if (!bodyParts.isEmpty()) {
        return Optional.ofNullable(bodyParts[0])
      }
    }

    return Optional.empty()
  }

  private fun getPartNames(elementExtensible: ElementExtensible): List<String> {
    val elements = elementExtensible.extensibilityElements
    val partNames = elements
      .filter { e -> e is SOAPBody || e is SOAP12Body }
      .map { e -> if (e is SOAPBody) e.parts else (e as SOAP12Body).parts }
      .flatMap { parts -> parts ?: emptyList<String>() } as List<String>

    if (!partNames.isEmpty()) {
      return partNames
    }

    val multiparts = elements.filter { e -> e is MIMEMultipartRelated }
    if (!multiparts.isEmpty()) {
      val multipart = multiparts[0] as MIMEMultipartRelated
      return multipart.mimeParts.map { e -> getPartNames(e as ElementExtensible) }.flatten()
    }
    return emptyList()
  }

  private fun getHeaderParts(bindingType: ElementExtensible): List<SoapHeader> {
    val extensible = bindingType.extensibilityElements
    if (extensible != null) {
      return extensible
        .filter { e -> e is SOAPHeader || e is SOAP12Header }
        .map { e -> if (e is SOAPHeader) SoapHeader(e) else SoapHeader(e as SOAP12Header) }
    }
    return emptyList()
  }

  private fun getAttachmentFields(containerType: MetadataType): List<ObjectFieldType> {
    val bodyType = getOperationType(containerType)
    if (bodyType is ObjectType) {
      return bodyType.fields.filter { field -> field.value is BinaryType }
    }
    return emptyList()
  }

  private fun getOperationType(bodyTypeContainer: MetadataType): MetadataType {
    if (bodyTypeContainer is ObjectType) {
      val bodyFields = bodyTypeContainer.fields
      if (bodyFields.size == 1) {
        // Contains only one field which represents de operation
        return bodyFields.iterator().next().value
      }
    }
    throw IllegalStateException("Could not find soap operation element in the provided body MetadataType")
  }

  private fun buildAttachments(): MetadataType {
    val bodyPart = getBodyPart()
    if (bodyPart != null) {
      val bodyType = buildPartMetadataType(bodyPart)
      val attachments = getAttachmentFields(bodyType)
      if (attachments.isEmpty()) {
        return getMultipartAttachments(bodyPart)
      }
      // TODO(MULE-15275): This piece of code should be removed when soap with attachments are no longer parsed.
      val type = BaseTypeBuilder.create(JAVA).objectType()
      attachments.forEach { attachment ->
        type.addField()
          .key(getLocalPart(attachment))
          .value(attachment.value)
      }
      return type.build()
    }
    return NULL_TYPE
  }

  private fun getMultipartAttachments(bodyPart: Part): MetadataType {
    if (message != null) {
      val parts = message.parts
      if (parts != null) {
        val type = BaseTypeBuilder.create(JAVA).objectType()
        parts.forEach { partName, partObject ->
          if (bodyPart.name != partName) {
            val typeName = (partObject as Part).typeName
            if (typeName != null && typeName.toString().toLowerCase().contains("binary")) {
              type.addField().key(partName as String).value().binaryType()
            }
          }
        }
        val result = type.build()

        if (!result.fields.isEmpty()) return result
      }
    }
    return NULL_TYPE
  }

  private fun buildOrGetDefaultType(func: () -> MetadataType): MetadataType {
    return try {
      func.invoke()
    } catch (e: Exception) {
      LOGGER.error("Error building operation [$name] type" + e.message, e); ANY_TYPE
    }
  }
}
