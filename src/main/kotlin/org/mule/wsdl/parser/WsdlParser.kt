package org.mule.wsdl.parser

import com.ibm.wsdl.extensions.schema.SchemaSerializer
import org.mule.wsdl.parser.exception.WsdlParsingException
import org.mule.wsdl.parser.locator.NullResourceLocator
import org.mule.wsdl.parser.locator.ResourceLocator
import org.mule.wsdl.parser.model.*
import org.mule.wsdl.parser.model.operation.OperationModel
import javax.wsdl.*
import javax.wsdl.extensions.ExtensionRegistry
import javax.wsdl.extensions.http.HTTPAddress
import javax.wsdl.extensions.mime.MIMEPart
import javax.wsdl.extensions.soap.SOAPAddress
import javax.wsdl.extensions.soap.SOAPBinding
import javax.wsdl.extensions.soap12.SOAP12Address
import javax.wsdl.extensions.soap12.SOAP12Binding
import javax.wsdl.factory.WSDLFactory
import javax.wsdl.xml.WSDLLocator
import javax.xml.namespace.QName


class WsdlParser private constructor(wsdlLocator: WSDLLocator) {

  private val definition = parseWsdl(wsdlLocator)
  private val wsdl = WsdlModel(wsdlLocator.baseURI, parseServices(definition), WsdlSchemasCollector(definition), definition)

  private fun parseServices(definition: Definition) = definition.services
      .map { (_, v) -> v as Service }
      .map { s -> ServiceModel(s.qName.localPart, s.qName, parsePorts(s)) }

  private fun parsePorts(service: Service): List<PortModel> = service.ports
      .map { (_, v) -> v as Port }
      .map { p -> PortModel(p.name, parseOperations(p), findSoapAddress(p), findPortBinding(p)) }

  private fun parseOperations(port: Port): List<OperationModel> = port.binding.bindingOperations
      .map { v -> v as BindingOperation }
      .map { bop -> OperationModel(bop.name, bop) }

  private fun parseWsdl(wsdlLocator: WSDLLocator): Definition {
    try {
      val factory = WSDLFactory.newInstance()
      val registry = initExtensionRegistry(factory)
      val wsdlReader = factory.newWSDLReader()
      wsdlReader.setFeature("javax.wsdl.verbose", false)
      wsdlReader.setFeature("javax.wsdl.importDocuments", true)
//      wsdlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
//      wsdlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
//      wsdlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
      wsdlReader.extensionRegistry = registry
      return wsdlReader.readWSDL(wsdlLocator)
    } catch (e: WSDLException) {
      // This replacement is made because the WSDLException outputs an ugly message, but we need a part of it.
      val msg = e.message?.replace("WSDLException:", "")?.replace("faultCode=OTHER_ERROR:", "")?.trim() ?: "UNKNOWN"
      throw WsdlParsingException("Error processing WSDL file [${wsdlLocator.baseURI}]: $msg", e)
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

  private fun findPortBinding(p: Port): SoapBinding? {
    return p.binding.extensibilityElements
        .filter { e -> e is SOAP12Binding || e is SOAPBinding }
        .map { e ->
          if (e is SOAP12Binding) {
            val style = e.style
            if (style != null) SoapBinding(WsdlStyleFinder.find(style), e.transportURI) else null
          }
          else {
            val style = (e as SOAPBinding).style
            if (style != null) SoapBinding(WsdlStyleFinder.find(style), e.transportURI) else null
          }
        }
        .firstOrNull()
  }

  companion object {
    fun parse(wsdlLocation: String): WsdlModel = WsdlParser(WsdlLocator(wsdlLocation, NullResourceLocator())).wsdl
    fun parse(wsdlLocation: String, locator: ResourceLocator): WsdlModel = WsdlParser(WsdlLocator(wsdlLocation, locator)).wsdl
  }
}
