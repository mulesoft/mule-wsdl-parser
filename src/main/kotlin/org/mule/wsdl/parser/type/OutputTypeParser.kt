package org.mule.wsdl.parser.type

import org.mule.metadata.xml.api.SchemaCollector
import javax.wsdl.BindingOperation
import javax.wsdl.Definition

internal class OutputTypeParser(definition: Definition, collector: SchemaCollector) : TypeParser(definition, collector) {

  override fun getExtensibilityElements(bop: BindingOperation) = bop.bindingOutput.extensibilityElements ?: emptyList<Any>()

  override fun getMessage(bop: BindingOperation) = bop.operation.output.message
}
