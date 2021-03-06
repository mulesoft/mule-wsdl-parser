package org.mule.wsdl.parser.model.operation

import org.mule.metadata.api.model.MetadataType
import org.mule.wsdl.parser.model.message.MessageDefinition
import org.mule.wsdl.parser.operation.WsdlOperationTypeParser.Companion.NULL_TYPE

class Type(val body: MetadataType,
           val headers: MetadataType,
           val attachments: MetadataType,
           val message: MessageDefinition) {
  companion object {
    val NULL_OPERATION_TYPE = Type(NULL_TYPE, NULL_TYPE, NULL_TYPE, MessageDefinition("null", emptyList()))
  }
}
