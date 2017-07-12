package org.mule.runtime.wsdl.parser.type

import org.mule.metadata.xml.SchemaCollector
import org.mule.runtime.wsdl.parser.WsdlSchemaCollector
import javax.wsdl.BindingOperation
import javax.wsdl.Definition

internal class InputTypeParser(definition: Definition, collector: SchemaCollector): TypeParser(definition, collector) {

  override fun getExtensibilityElements(bop: BindingOperation) = bop.bindingInput.extensibilityElements

  override fun getMessage(bop: BindingOperation) = bop.operation.input.message

}
