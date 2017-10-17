package org.mule.wsdl.parser.model.operation

import javax.wsdl.extensions.soap.SOAPHeader
import javax.wsdl.extensions.soap12.SOAP12Header
import javax.xml.namespace.QName

class SoapHeader(val qName: QName, val partName: String, val namespace: String?) {

  constructor(header: SOAPHeader) : this(header.message, header.part, header.namespaceURI)
  constructor(header: SOAP12Header) : this(header.message, header.part, header.namespaceURI)
}
