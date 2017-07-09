package org.mule.runtime.wsdl.parser

import org.mule.metadata.xml.SchemaCollector
import org.mule.service.soap.util.XmlTransformationException
import java.util.*
import javax.wsdl.Definition
import javax.wsdl.Import
import javax.wsdl.Types
import javax.wsdl.extensions.schema.Schema
import javax.wsdl.extensions.schema.SchemaImport
import javax.wsdl.extensions.schema.SchemaReference

internal class WsdlSchemaCollector(private val definition: Definition) {

  private val schemas: MutableMap<String, Schema> = hashMapOf()

  fun collect(): SchemaCollector {
    val collector = SchemaCollector.getInstance()
    collectSchemas(definition)
    schemas.forEach({ uri, schema ->
      try {
//        collector.addSchema(uri, nodeToString(schema.getElement()))
      } catch (e: XmlTransformationException) {
        val message = if (uri.endsWith(".wsdl")) "Schema embedded in wsdl $uri" else "Schema $uri"
        throw RuntimeException("$message could not be parsed", e)
      }
    })
    return collector
  }

  private fun collectSchemas(definition: Definition) {
    collectFromTypes(definition.types)
    definition.imports.values.forEach { wsdlImport ->
      if (wsdlImport is Import) {
        collectSchemas(wsdlImport.definition)
      }
    }
  }

  private fun collectFromTypes(types: Types?) {
    types?.extensibilityElements?.forEach { element ->
      if (element is Schema) {
        addSchema(element)
      }
    }
  }

  private fun addSchema(schema: Schema) {
    val key = schema.documentBaseURI
    if (!schemas.containsKey(key)) {
      schemas.put(key, schema)
      addImportedSchemas(schema)
      addIncludedSchemas(schema)
    }
  }

  private fun addImportedSchemas(schema: Schema) {
    val imports = schema.imports.values
    imports.forEach { vector ->
      (vector as Vector<*>).forEach { element ->
        if (element is SchemaImport) {
          val importedSchema = element.referencedSchema
          addSchema(importedSchema)
        }
      }
    }
  }

  private fun addIncludedSchemas(schema: Schema) {
    schema.includes.forEach { include ->
      if (include is SchemaReference) {
        addSchema(include.referencedSchema)
      }
    }
  }
}
