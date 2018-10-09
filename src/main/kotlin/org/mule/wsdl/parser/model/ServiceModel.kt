package org.mule.wsdl.parser.model

import javax.xml.namespace.QName

class ServiceModel(override val name: String, val qName: QName, val ports: List<PortModel>) : NamedModel {
  fun getPort(name: String): PortModel? = ports.find { port -> port.name == name }
}
