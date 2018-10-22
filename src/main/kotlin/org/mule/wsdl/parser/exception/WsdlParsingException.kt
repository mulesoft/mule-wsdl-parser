package org.mule.wsdl.parser.exception

class WsdlParsingException(message: String?, cause: Throwable?) : Exception(message, cause) {
  constructor(message: String?) : this(message, null)
}
