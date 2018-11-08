package org.mule.wsdl.parser.model

import org.mule.wsdl.parser.exception.OperationNotFoundException
import org.mule.wsdl.parser.model.operation.OperationModel

class PortModel(override val name: String,
                val operations: List<OperationModel>,
                val address: String?,
                val binding: SoapBinding?) : NamedModel {

  fun getOperation(name: String): OperationModel {
    return operations.find { ope -> ope.name == name } ?: throw OperationNotFoundException("operation [$name] was not found in the current wsdl file.")
  }

  fun getOperationsMap(): Map<String, OperationModel> {
    return operations.map { it.name to it }.toMap()
  }
}
