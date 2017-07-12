package org.mule.runtime.wsdl.parser.model

import java.io.InputStream
import javax.wsdl.Definition
import javax.wsdl.Service

class WsdlModel(val location: String, val services: List<ServiceModel>, val schemas: Map<String, String>) {
  fun getService(name: String): ServiceModel? = services.find { service -> service.name == name }
}
