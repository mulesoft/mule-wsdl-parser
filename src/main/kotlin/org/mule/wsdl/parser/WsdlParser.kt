package org.mule.wsdl.parser

import com.ibm.wsdl.extensions.schema.SchemaSerializer
import org.mule.metadata.xml.api.XmlTypeLoader
import org.mule.wsdl.parser.exception.WsdlParsingException
import org.mule.wsdl.parser.locator.GlobalResourceLocator
import org.mule.wsdl.parser.locator.ResourceLocator
import org.mule.wsdl.parser.locator.WsdlLocator
import org.mule.wsdl.parser.model.PortModel
import org.mule.wsdl.parser.model.ServiceModel
import org.mule.wsdl.parser.model.SoapBinding
import org.mule.wsdl.parser.model.WsdlModel
import org.mule.wsdl.parser.model.WsdlStyle
import org.mule.wsdl.parser.model.WsdlStyleFinder
import org.mule.wsdl.parser.model.message.MessageDefinition
import org.mule.wsdl.parser.model.operation.OperationModel
import org.mule.wsdl.parser.model.version.SoapVersion
import org.mule.wsdl.parser.operation.DefaultWsdlOperationParser
import java.security.MessageDigest
import javax.wsdl.BindingInput
import javax.wsdl.BindingOperation
import javax.wsdl.Definition
import javax.wsdl.Message
import javax.wsdl.Port
import javax.wsdl.Service
import javax.wsdl.Types
import javax.wsdl.WSDLException
import javax.wsdl.extensions.ExtensionRegistry
import javax.wsdl.extensions.http.HTTPAddress
import javax.wsdl.extensions.mime.MIMEPart
import javax.wsdl.extensions.soap.SOAPAddress
import javax.wsdl.extensions.soap.SOAPBinding
import javax.wsdl.extensions.soap.SOAPOperation
import javax.wsdl.extensions.soap12.SOAP12Address
import javax.wsdl.extensions.soap12.SOAP12Binding
import javax.wsdl.extensions.soap12.SOAP12Operation
import javax.wsdl.factory.WSDLFactory
import javax.wsdl.xml.WSDLLocator
import javax.wsdl.xml.WSDLReader
import javax.xml.namespace.QName


open class WsdlParser internal constructor(private val wsdlLocator: WSDLLocator, private val charset: String = "UTF-8") {

  private val definition = parseWsdl()

  private val loader = XmlTypeLoader(WsdlSchemasCollector(definition, charset).collector())
  internal val style = findStyle()
  /**
   * The parsed WsdlModel
   */
  val wsdl = WsdlModel(wsdlHashString(), wsdlLocator.baseURI, definition.targetNamespace, parseServices(definition), style, parseMessages(definition))

  /**
   * Collects all the schemas associated to the provided wsdl file.
   *
   * @return a map with the name of the schema as key and the content of the schema as value.
   */
  fun collectSchemas(): Map<String, String> = WsdlSchemasCollector(definition, charset).toInMemorySchemaMap()

  private fun parseWsdl(): Definition {
    try {
      val factory = WSDLFactory.newInstance()
      val registry = initExtensionRegistry(factory)
      val wsdlReader = factory.newWSDLReader()
      setFeatures(wsdlReader)
      wsdlReader.extensionRegistry = registry
      return wsdlReader.readWSDL(wsdlLocator)
    } catch (e: WSDLException) {
      // This replacement is made because the WSDLException outputs an ugly message, but we need a part of it.
      val msg = e.message?.replace("WSDLException:", "")?.replace("faultCode=OTHER_ERROR:", "")?.trim() ?: "UNKNOWN"
      throw WsdlParsingException("Error processing WSDL file [${wsdlLocator.baseURI}]: $msg", e)
    }
  }

  private fun wsdlHashString(): String {
    return MessageDigest
      .getInstance("SHA-256")
      .digest(wsdlLocator.baseInputSource.byteStream.readBytes())
      .map { Integer.toHexString(it.toInt()) }
      .joinToString(separator = "")
  }

  private fun parseMessages(definition: Definition): Set<MessageDefinition> {
    return definition.messages.values.map { m -> MessageDefinition.fromMessage(m as Message, null) }.toSet()
  }

  private fun parseServices(definition: Definition) = definition.services
    .map { (_, v) -> v as Service }
    .map { s -> ServiceModel(s.qName.localPart, s.qName, parsePorts(s)) }

  private fun parsePorts(service: Service): List<PortModel> = service.ports
    .map { (_, v) -> v as Port }
    .map { p ->
      val binding = findPortBinding(p)
      PortModel(p.name, parseOperations(p, binding), findSoapAddress(p), binding)
    }

  internal open fun parseOperations(port: Port, binding: SoapBinding?): List<OperationModel> = port.binding.bindingOperations
    .map { bop -> DefaultWsdlOperationParser.parse(definition, style, loader, bop as BindingOperation) }

  internal open fun setFeatures(wsdlReader: WSDLReader) {
    wsdlReader.setFeature("javax.wsdl.verbose", false)
    wsdlReader.setFeature("javax.wsdl.importDocuments", true)
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
          val style = e.style ?: "document"
          SoapBinding(SoapVersion.SOAP12, WsdlStyleFinder.find(style), e.transportURI)
        } else {
          val style = (e as SOAPBinding).style ?: "document"
          SoapBinding(SoapVersion.SOAP11, WsdlStyleFinder.find(style), e.transportURI)
        }
      }
      .firstOrNull()
  }

  private fun findStyle(): WsdlStyle {
    val ports = definition.services.values.flatMap { s -> (s as Service).ports.values as Collection<Port> }
    val bindingStyle = ports.flatMap { it.binding.extensibilityElements }.filter { e -> e is SOAPBinding }.map { b -> (b as SOAPBinding).style }.firstOrNull()
    if (bindingStyle != null) {
      return WsdlStyleFinder.find(bindingStyle)
    }

    val operationStyle = ports.flatMap { p -> p.binding.bindingOperations as List<BindingOperation> }
      .flatMap { ope -> ope.extensibilityElements }
      .filter { e -> e is SOAP12Operation || e is SOAPOperation }
      .map { e -> if (e is SOAPOperation) e.style else (e as SOAP12Operation).style }
      .firstOrNull()

    if (operationStyle != null) {
      return WsdlStyleFinder.find(operationStyle)
    }
    return WsdlStyle.DOC_LITERAL
  }

  companion object {

    @Deprecated("this method is deprecated, use another one that receives a charset")
    fun parse(wsdlLocation: String): WsdlModel = WsdlParser(WsdlLocator(wsdlLocation, GlobalResourceLocator())).wsdl

    @Deprecated("this method is deprecated, use another one that receives a charset")
    fun parse(wsdlLocation: String, locator: ResourceLocator): WsdlModel = WsdlParser(WsdlLocator(wsdlLocation, locator)).wsdl

    fun parse(wsdlLocation: String, charset: String): WsdlModel = WsdlParser(WsdlLocator(wsdlLocation, GlobalResourceLocator()), charset).wsdl
    fun parse(wsdlLocation: String, locator: ResourceLocator, charset: String): WsdlModel = WsdlParser(WsdlLocator(wsdlLocation, locator), charset).wsdl

    fun instance(wsdlLocation: String, charset: String): WsdlParser = WsdlParser(WsdlLocator(wsdlLocation, GlobalResourceLocator()), charset)
    fun instance(wsdlLocation: String, locator: ResourceLocator, charset: String): WsdlParser = WsdlParser(WsdlLocator(wsdlLocation, locator), charset)
  }
}
