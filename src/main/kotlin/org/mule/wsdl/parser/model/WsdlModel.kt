package org.mule.wsdl.parser.model

import org.mule.wsdl.parser.exception.OperationNotFoundException
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

  fun getOperation(name: String): OperationModel? {
    val operations = services.flatMap { it.ports }.flatMap { it.operations }
    if (operations.size > 1) {
      throw OperationNotFoundException("Multiple operations [$name] found, the operation may be defined in multiple ports")
    }
    return operations.firstOrNull()
  }

}
