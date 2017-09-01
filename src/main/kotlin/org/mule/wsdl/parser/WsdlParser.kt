package org.mule.wsdl.parser

import com.ibm.wsdl.extensions.schema.SchemaSerializer
import org.mule.wsdl.parser.model.OperationModel
import org.mule.wsdl.parser.model.PortModel
import org.mule.wsdl.parser.model.ServiceModel
import org.mule.wsdl.parser.model.WsdlModel
import org.mule.wsdl.parser.type.InputTypeParser
import org.mule.wsdl.parser.type.OutputTypeParser
import javax.wsdl.*
import javax.wsdl.extensions.ExtensionRegistry
import javax.wsdl.extensions.http.HTTPAddress
import javax.wsdl.extensions.mime.MIMEPart
import javax.wsdl.extensions.soap.SOAPAddress
import javax.wsdl.extensions.soap12.SOAP12Address
import javax.wsdl.factory.WSDLFactory
import javax.xml.namespace.QName

class WsdlParser private constructor(wsdlLocation: String) {

  private val definition = parseWsdl(wsdlLocation)
  private val schemaCollector = WsdlSchemaCollector(definition)
  private val inputTypeParser = InputTypeParser(definition, schemaCollector.getCollector())
  private val outputTypeParser = OutputTypeParser(definition, schemaCollector.getCollector())
  internal val wsdl = WsdlModel(wsdlLocation, parseServices(definition), schemaCollector.parsedSchemas)

  private fun parseServices(definition: Definition) = definition.services
      .map { (_, v) -> v as Service }
      .map { s -> ServiceModel(s.qName.localPart, s.qName, parsePorts(s)) }

  private fun parsePorts(service: Service): List<PortModel> = service.ports
      .map { (_, v) -> v as Port }
      .map { p -> PortModel(p.name, parseOperations(p), findSoapAddress(p)) }

  private fun parseOperations(port: Port): List<OperationModel> = port.binding.bindingOperations
      .map { v -> v as BindingOperation }
      .map { bop -> OperationModel(bop.name, inputTypeParser.getParts(bop), outputTypeParser.getParts(bop)) }

  private fun parseWsdl(location: String): Definition {
    try {
      val factory = WSDLFactory.newInstance()
      val registry = initExtensionRegistry(factory)
      val wsdlReader = factory.newWSDLReader()
      wsdlReader.extensionRegistry = registry
      wsdlReader.setFeature("javax.wsdl.verbose", false)
      wsdlReader.setFeature("javax.wsdl.importDocuments", true)
      return wsdlReader.readWSDL(location)
    } catch (exception: WSDLException) {
      throw RuntimeException("Could not parse wsdl: $location", exception)
    }
  }

  private fun initExtensionRegistry(factory: WSDLFactory): ExtensionRegistry {
    val registry = factory.newPopulatedExtensionRegistry()
    registry.registerSerializer(Types::class.java, QName("http://www.w3.org/2001/XMLSchema", "schema"), SchemaSerializer())
    val header = QName("http://schemas.xmlsoap.org/wsdl/soap/", "header")
    registry.registerDeserializer(MIMEPart::class.java, header, registry.queryDeserializer(BindingInput::class.java, header))
    registry.registerSerializer(MIMEPart::class.java, header, registry.querySerializer(BindingInput::class.java, header))
    val clazz = registry.createExtension(BindingInput::class.java, header)::class.java
    registry.mapExtensionTypes(MIMEPart::class.java, header, clazz)
    return registry
  }

  private fun findSoapAddress(port: Port): String? {
    for (element in port.extensibilityElements) {
      if (element is SOAPAddress) {
        return element.locationURI
      } else if (element is SOAP12Address) {
        return element.locationURI
      } else if (element is HTTPAddress) {
        return element.locationURI
      }
    }
    return null
  }

  companion object {
    fun parse(wsdlLocation: String): WsdlModel = WsdlParser(wsdlLocation).wsdl
  }
}
