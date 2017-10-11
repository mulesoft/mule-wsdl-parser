package org.mule.wsdl.parser.model

enum class WsdlStyle(name: String) {
  DOC_LITERAL("document"),
  RPC("rpc")
}

object WsdlStyleFinder {

  fun find(value: String) : WsdlStyle = when(value) {
    "rpc" -> WsdlStyle.RPC
    "RPC" -> WsdlStyle.RPC
    else -> WsdlStyle.DOC_LITERAL
  }
}
