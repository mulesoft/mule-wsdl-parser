package org.mule.runtime.wsdl.parser.type

import org.mule.metadata.api.TypeLoader
import org.mule.metadata.api.builder.BaseTypeBuilder
import org.mule.metadata.api.model.MetadataFormat
import org.mule.metadata.xml.SchemaCollector
import org.mule.metadata.xml.XmlTypeLoader
import org.mule.runtime.wsdl.parser.model.MessagePartModel
import org.mule.runtime.wsdl.parser.model.MessagePartModel.PartType
import javax.wsdl.BindingOperation
import javax.wsdl.Definition
import javax.wsdl.Message
import javax.wsdl.Part
import javax.wsdl.extensions.soap.SOAPHeader
import javax.wsdl.extensions.soap12.SOAP12Header
import javax.xml.namespace.QName
abstract class TypeParser(private val definition: Definition, collector: SchemaCollector) {

  companion object {
    private val STRING_TYPE = BaseTypeBuilder.create(MetadataFormat.XML).stringType().build()
  }

  val loader: TypeLoader = XmlTypeLoader(collector)

  fun getParts(bop: BindingOperation): List<MessagePartModel> {
    return getMessage(bop)?.parts?.map { (_, p) -> toMessagePartModel(p as Part, getPartType(bop, p)) } ?: emptyList()
  }

  private fun getPartType(bop: BindingOperation, part:Part): PartType {
    val parts = mutableListOf<String>()
    getExtensibilityElements(bop).forEach({ e ->
      when(e) {
        is SOAPHeader -> parts.add(e.part)
        is SOAP12Header -> parts.add(e.part)
      }
    })
    return if (parts.contains(part.name)) PartType.HEADER else PartType.BODY
  }

  private fun toMessagePartModel(part: Part, partType: PartType): MessagePartModel {
    if (part.elementName != null) {
      val partName = part.elementName.toString()
      val type = loader.load(partName).orElseThrow({ RuntimeException("Could not load part element name [$partName]") })
      return MessagePartModel(partName, type, partType)
    }
    if (part.typeName != null) {
      return MessagePartModel(part.name, STRING_TYPE, partType)
    }
    throw RuntimeException("Cannot retrieve metadata type for a nameless part")
  }

  private fun getMessageForName(qName: QName): Message = definition.getMessage(qName)

  abstract fun getExtensibilityElements(bop: BindingOperation): List<Any?>

  abstract fun getMessage(bop: BindingOperation): Message?
}

