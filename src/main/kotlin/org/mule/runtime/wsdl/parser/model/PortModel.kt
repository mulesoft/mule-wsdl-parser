package org.mule.runtime.wsdl.parser.model

import java.net.URL

class PortModel(override val name: String, val operations: List<OperationModel>, address: String?) : NamedModel {
  val address: URL? = parseAddress(address)

  fun getOperation(name: String): OperationModel? = operations.find { ope -> ope.name == name }

  private fun parseAddress(address: String?): URL? = try { URL(address) } catch(e: Exception) { null }
}
