package org.mule.wsdl.parser.exception

class OperationNotFoundException(name: String): RuntimeException("operation [$name] was not found in the current wsdl file.")
