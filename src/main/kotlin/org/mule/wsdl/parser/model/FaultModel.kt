package org.mule.wsdl.parser.model

import org.mule.wsdl.parser.model.message.MessageDefinition
import javax.wsdl.Fault

class FaultModel(override val name: String, val message: MessageDefinition): NamedModel
