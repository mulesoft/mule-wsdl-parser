package org.mule.wsdl.parser.model.operation

import org.apache.commons.lang3.StringUtils.isBlank
import org.mule.wsdl.parser.model.FaultModel
import org.mule.wsdl.parser.model.NamedModel
import org.mule.wsdl.parser.model.WsdlStyle
import org.mule.wsdl.parser.model.WsdlStyleFinder
import java.util.*
import java.util.Optional.ofNullable
import javax.wsdl.*
import javax.wsdl.extensions.ElementExtensible
import javax.wsdl.extensions.mime.MIMEMultipartRelated
import javax.wsdl.extensions.soap.SOAPBody
import javax.wsdl.extensions.soap.SOAPHeader
import javax.wsdl.extensions.soap.SOAPOperation
import javax.wsdl.extensions.soap12.SOAP12Body
import javax.wsdl.extensions.soap12.SOAP12Header
import javax.wsdl.extensions.soap12.SOAP12Operation

class OperationModel(override val name: String, private val bop: BindingOperation) : NamedModel {

  val style: WsdlStyle? = findStyle()
  val type: OperationType = findType()
  val soapAction: Optional<String> = findAction()

  fun getInputMessage(): Message {
    return bop.operation.input.message
  }

  fun getOutputMessage(): Message {
    return bop.operation.output.message
  }

  fun getInputBodyPart(): Optional<Part> {
    return getBodyPart(getInputMessage(), bop.bindingInput)
  }

  fun getInputParts(): List<String> {
    return getPartNames(bop.bindingInput)
  }

  fun getOutputParts(): List<String> {
    return getPartNames(bop.bindingOutput)
  }

  fun getOutputBodyPart(): Optional<Part> {
    return getBodyPart(getOutputMessage(), bop.bindingOutput)
  }

  fun getInputHeaders(): List<SoapHeader> {
    return getHeaderParts(bop.bindingInput)
  }

  fun getOutputHeaders(): List<SoapHeader> {
    return getHeaderParts(bop.bindingOutput)
  }

  fun getFault(faultName: String): FaultModel {
    try {
      return FaultModel(bop.operation.getFault(faultName))
    } catch (e: Exception) {
      throw IllegalArgumentException("Could not retrieve fault [$name]", e)
    }
  }

  fun getFaults(): List<FaultModel> = bop.operation.faults.map { (_, v) -> FaultModel(v as Fault) }

  private fun findType(): OperationType {
    return when(bop.operation.style) {
      javax.wsdl.OperationType.NOTIFICATION -> OperationType.NOTIFICATION
      javax.wsdl.OperationType.SOLICIT_RESPONSE -> OperationType.SOLICIT_RESPONSE
      javax.wsdl.OperationType.ONE_WAY -> OperationType.ONE_WAY
      else -> OperationType.REQUEST_RESPONSE
    }
  }

  private fun findStyle(): WsdlStyle? {
    val style = bop.extensibilityElements
        .filter { e -> e is SOAP12Operation || e is SOAPOperation }
        .map { e -> if (e is SOAPOperation) e.style else (e as SOAP12Operation).style }
        .filter { it != null }
        .firstOrNull()
    return if (style != null) WsdlStyleFinder.find(style) else null
  }

  private fun findAction(): Optional<String> {
    val action = bop.extensibilityElements
        .filter { e -> e is SOAP12Operation || e is SOAPOperation }
        .map { e -> if (e is SOAPOperation) e.soapActionURI else (e as SOAP12Operation).soapActionURI }
        .filter { it != null }
        .firstOrNull()
    return if (isBlank(action)) Optional.empty() else ofNullable(action)
  }

  private fun getBodyPart(message: Message, bindingType: ElementExtensible): Optional<Part> {
    val parts = message.parts
    if (parts == null || parts.isEmpty()) {
      return Optional.empty()
    }
    if (parts.size == 1) {
      return ofNullable(parts[parts.keys.toTypedArray()[0]] as Part)
    }
    return getBodyPartName(bindingType).flatMap<Part> { partName -> ofNullable(parts[partName] as Part) }
  }

  private fun getBodyPartName(bindingType: ElementExtensible): Optional<String> {
    val elements = bindingType.extensibilityElements
    if (elements != null) {
      val bodyParts = getPartNames(bindingType)
      if (!bodyParts.isEmpty()) {
        return ofNullable(bodyParts[0])
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
      return multipart.mimeParts.map({ e -> getPartNames(e as ElementExtensible) }).flatten()
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
}
