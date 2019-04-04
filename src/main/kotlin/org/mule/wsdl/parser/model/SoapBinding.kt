package org.mule.wsdl.parser.model

import org.mule.wsdl.parser.model.version.SoapVersion

class SoapBinding(val version: SoapVersion, val style: WsdlStyle, val transportUri: String)
