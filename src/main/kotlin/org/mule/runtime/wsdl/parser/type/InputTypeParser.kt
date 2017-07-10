package org.mule.runtime.wsdl.parser.type

import javax.wsdl.BindingOperation
import javax.wsdl.Definition
import javax.wsdl.Part
import javax.wsdl.extensions.soap.SOAPBody
import javax.wsdl.extensions.soap12.SOAP12Body

internal class InputTypeParser(definition: Definition): TypeParser(definition) {

  override fun getExtensibilityElements(bop: BindingOperation) = bop.bindingInput.extensibilityElements

  override fun getMessage(bop: BindingOperation) = bop.operation.input.message

}
