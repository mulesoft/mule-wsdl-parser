package org.mule.wsdl.parser.model.message

import javax.wsdl.Message
import javax.wsdl.Part

class MessageDefinition(val name: String, val parts: List<MessagePart>) {

    fun getPart(name: String): MessagePart? = parts.find { p -> name == p.name }

    companion object {
        fun fromMessage(m: Message) : MessageDefinition {
           return MessageDefinition(m.qName.toString(), m.parts.map { (_, p) -> p as Part }
                   .map { part -> MessagePart(part.name, part.elementName, part.typeName) })
        }
    }
}
