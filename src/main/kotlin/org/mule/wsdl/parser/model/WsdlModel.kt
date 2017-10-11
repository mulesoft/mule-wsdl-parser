package org.mule.wsdl.parser.model

import org.mule.metadata.api.TypeLoader
import javax.wsdl.Definition
import javax.xml.namespace.QName

class WsdlModel(val location: String,
                val services: List<ServiceModel>,
                val loader: TypeLoader,
                private val definition: Definition) {

  val style = findStyle()

  fun getService(name: String): ServiceModel? = services.find { service -> service.name == name }

  fun isWsdlStyle(style: WsdlStyle) = style == this.style

 fun getMessage(qname: QName) = definition.getMessage(qname)

  private fun findStyle(): WsdlStyle {
    val ports = services.flatMap { it.ports }
    val bindingStyle = ports.find { it.binding != null }?.binding?.style
    if (bindingStyle != null) {
      return bindingStyle
    }
    val operationStyle = ports.flatMap { it.operations }.map { it.style }.filter { it != null }.firstOrNull()
    if (operationStyle != null) {
      return operationStyle
    }
    return WsdlStyle.DOC_LITERAL
  }
}
