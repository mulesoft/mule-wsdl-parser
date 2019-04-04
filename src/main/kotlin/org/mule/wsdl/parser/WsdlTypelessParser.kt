package org.mule.wsdl.parser

import com.ibm.wsdl.Constants.FEATURE_PARSE_SCHEMA
import org.mule.wsdl.parser.locator.GlobalResourceLocator
import org.mule.wsdl.parser.locator.ResourceLocator
import org.mule.wsdl.parser.locator.WsdlLocator
import org.mule.wsdl.parser.model.SoapBinding
import org.mule.wsdl.parser.model.WsdlModel
import org.mule.wsdl.parser.model.operation.OperationModel
import org.mule.wsdl.parser.operation.WsdlTypelessOperationParser
import javax.wsdl.BindingOperation
import javax.wsdl.Port
import javax.wsdl.xml.WSDLLocator
import javax.wsdl.xml.WSDLReader

class WsdlTypelessParser(wsdlLocator: WSDLLocator, charset: String = "UTF-8"): WsdlParser(wsdlLocator, charset) {

  override fun parseOperations(port: Port, binding: SoapBinding?): List<OperationModel> {
    return port.binding.bindingOperations.map { bop -> WsdlTypelessOperationParser.parse(style, bop as BindingOperation) }
  }

  override fun setFeatures(wsdlReader: WSDLReader) {
    super.setFeatures(wsdlReader)
    wsdlReader.setFeature(FEATURE_PARSE_SCHEMA, false)
  }

  companion object {
    fun parse(wsdlLocation: String): WsdlModel = WsdlTypelessParser(WsdlLocator(wsdlLocation, GlobalResourceLocator())).wsdl
    fun parse(wsdlLocation: String, charset: String): WsdlModel = WsdlTypelessParser(WsdlLocator(wsdlLocation, GlobalResourceLocator()), charset).wsdl
    fun parse(wsdlLocation: String, locator: ResourceLocator): WsdlModel = WsdlTypelessParser(WsdlLocator(wsdlLocation, locator)).wsdl
    fun parse(wsdlLocation: String, locator: ResourceLocator, charset: String): WsdlModel = WsdlTypelessParser(WsdlLocator(wsdlLocation, locator), charset).wsdl
  }
}
