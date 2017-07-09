package org.mule.runtime.wsdl.parser

object WsdlParserFactory {

  fun getParser(wsdlLocation: String) = WsdlParser(wsdlLocation);
}
