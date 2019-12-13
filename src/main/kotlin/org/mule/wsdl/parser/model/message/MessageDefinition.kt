package org.mule.wsdl.parser.model.message

import java.util.ArrayList
import javax.wsdl.Message
import javax.wsdl.Part
import javax.wsdl.extensions.ElementExtensible
import javax.wsdl.extensions.mime.MIMEContent
import javax.wsdl.extensions.mime.MIMEMultipartRelated
import javax.wsdl.extensions.mime.MIMEPart
import javax.wsdl.extensions.soap.SOAPHeader
import javax.wsdl.extensions.soap12.SOAP12Header

class MessageDefinition(val name: String, val parts: List<MessagePart>) {

  fun getPart(name: String): MessagePart? = parts.find { p -> name == p.name }

  companion object {
    fun fromMessage(m: Message, bindingType: ElementExtensible?): MessageDefinition {
      return MessageDefinition(m.qName.toString(), m.parts.map { (_, p) -> p as Part }
        .map { part ->
          MessagePart(part.name, part.elementName, part.typeName,
            isAttachmentPart(part, bindingType), isHeaderPart(part, bindingType, m))
        })
    }

    private fun isAttachmentPart(part: Part, bindingType: ElementExtensible?): Boolean {

      if (bindingType == null) {
        return false
      }

      val multipart = bindingType.extensibilityElements.filter { e -> e is MIMEMultipartRelated }.firstOrNull()

      val result = ArrayList<MIMEContent>()

      if (multipart != null) {
        val parts = (multipart as MIMEMultipartRelated).mimeParts as List<MIMEPart>

        for (c in parts.indices) {
          val contentParts = parts[c].extensibilityElements.filter { it is MIMEContent } as List<MIMEContent>

          for (content in contentParts) {
            if (content.part == part.name) {
              result.add(content)
            }
          }
        }
      }

      return !result.isEmpty()
    }

    private fun isHeaderPart(part: Part, bindingType: ElementExtensible?, message: Message): Boolean {

      if (bindingType == null){
        return false
      }

      return !bindingType.extensibilityElements.filter {
        (it is SOAPHeader && message.qName == it.message && part.name == it.part) ||
                (it is SOAP12Header && message.qName == it.message && part.name == it.part) }.isEmpty()
    }
  }
}
