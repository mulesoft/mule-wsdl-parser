package org.mule.wsdl.parser.model

import org.mule.metadata.xml.api.XmlTypeLoader
import org.mule.wsdl.parser.WsdlSchemasCollector
import org.mule.wsdl.parser.model.message.MessageDefinition
import org.mule.wsdl.parser.model.operation.OperationModel
import java.io.Serializable
import javax.xml.namespace.QName

class WsdlModel(val location: String,
                val services: List<ServiceModel>,
                val style: WsdlStyle,
                val messages: Set<MessageDefinition>) : Serializable {

  fun getService(name: String): ServiceModel? = services.find { service -> service.name == name }

  fun isWsdlStyle(style: WsdlStyle) = style == this.style

  fun getMessage(qname: QName) = messages.find { m -> qname.toString() == m.name }

  fun getOperation(name: String): OperationModel? = services.flatMap { it.ports }.flatMap { it.operations }.firstOrNull { it.name == name }

}
