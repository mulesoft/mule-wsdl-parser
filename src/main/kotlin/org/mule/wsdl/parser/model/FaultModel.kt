package org.mule.wsdl.parser.model

import org.mule.wsdl.parser.model.message.MessageDefinition

class FaultModel(override val name: String, val message: MessageDefinition) : NamedModel
