package org.mule.runtime.wsdl.parser.model

import javax.wsdl.Definition
import javax.wsdl.Service

class WsdlModel(val location: String, val services: List<ServiceModel>) {
  fun getService(name: String): ServiceModel? = services.find { service -> service.name == name }
}
