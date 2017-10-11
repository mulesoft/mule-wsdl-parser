package org.mule.wsdl.parser.model

import org.mule.wsdl.parser.model.operation.OperationModel
import java.net.URL

class PortModel(override val name: String,
                val operations: List<OperationModel>,
                address: String?,
                val binding: SoapBinding?) : NamedModel {

  val address: URL? = parseAddress(address)

  fun getOperation(name: String): OperationModel? = operations.find { ope -> ope.name == name }

  private fun parseAddress(address: String?): URL? = try { URL(address) } catch(e: Exception) { null }
}
