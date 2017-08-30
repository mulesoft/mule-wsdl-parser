package org.mule.wsdl.parser.model

class WsdlModel(val location: String, val services: List<ServiceModel>, val schemas: Map<String, String>) {
  fun getService(name: String): ServiceModel? = services.find { service -> service.name == name }
}
