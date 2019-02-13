package org.mule.wsdl.parser

import net.sf.saxon.jaxp.SaxonTransformerFactory
import org.mule.metadata.xml.api.SchemaCollector
import org.w3c.dom.Node
import java.io.StringWriter
import java.util.*
import javax.wsdl.Definition
import javax.wsdl.Import
import javax.wsdl.Types
import javax.wsdl.extensions.schema.Schema
import javax.wsdl.extensions.schema.SchemaImport
import javax.wsdl.extensions.schema.SchemaReference
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import kotlin.collections.HashMap


/**
 * The purpose of this class is to find all the schema URLs, both local or remote, for a given WSDL definition. This includes
 * imports and includes in the WSDL file and recursively in each schema found.
 *
 * @since 1.0
 */
@SuppressWarnings("unchecked")
class WsdlSchemasCollector(private val definition: Definition, private val charset: String = "UTF-8") {

  private val schemas = HashMap<String, Schema>()
  // we should keep track of already found imports to avoid recursive infinite loops
  private val foundImports = ArrayList<String>()

  fun collector(): SchemaCollector {

    val collector: SchemaCollector = try {
      SchemaCollector.getInstance(charset)
    } catch (e: NoSuchMethodError) {
      SchemaCollector.getInstance()
    }
    collectSchemas(definition)
    schemas.forEach { (uri, schema) ->
      try {
        collector.addSchema(uri, nodeToString(schema.element))
      } catch (e: Exception) {
        val schema = if (uri.endsWith(".wsdl")) "Schema embedded in wsdl" else "Schema"
        throw IllegalArgumentException("$schema [$uri] could not be parsed", e)
      }
    }
    return collector
  }

  private fun collectSchemas(definition: Definition) {
    collectFromTypes(definition.types)
    definition.imports.values.forEach { import ->
      if (import is Vector<*>) {
        import.forEach { it -> collectIfImport(it) }
      } else {
        collectIfImport(import)
      }
    }
  }

  private fun collectIfImport(import: Any?) {
    if (import is Import && !foundImports.contains(import.locationURI)) {
      foundImports.add(import.locationURI)
      collectSchemas(import.definition)
    }
  }

  private fun collectFromTypes(types: Types?) {
    types?.extensibilityElements?.forEach { element ->
      if (element is Schema) {
        // using hashcode since there is no way to distinct this schemas.
        addSchema(element.documentBaseURI + element.hashCode().toString(), element)
      }
    }
  }

  private fun addSchema(key: String, schema: Schema) {
    if (!schemas.containsKey(key)) {
      schemas.put(key, schema)
      addImportedSchemas(schema)
      addIncludedSchemas(schema)
    }
  }

  private fun addImportedSchemas(schema: Schema) {
    val imports = schema.imports.values
    imports.forEach { vector ->
      if (vector is Vector<*>) vector.forEach { element ->
        if (element is SchemaImport) {
          val importedSchema = element.referencedSchema
          if (importedSchema != null) {
            addSchema(importedSchema.documentBaseURI, importedSchema);
          }
        }
      }
    }
  }

  private fun addIncludedSchemas(schema: Schema) {
    schema.includes.forEach { include ->
      if (include is SchemaReference) {
        val referencedSchema = include.referencedSchema;
        addSchema(referencedSchema.documentBaseURI, referencedSchema)
      }
    }
  }

  private fun nodeToString(node: Node): String {
    try {
      val writer = StringWriter()
      val source = DOMSource(node)
      val result = StreamResult(writer)
      val factory = SaxonTransformerFactory()
      val transformer = factory.newTransformer()
      transformer.transform(source, result)
      return writer.toString()
    } catch (e: Exception) {
      throw RuntimeException("Error transforming node to String", e)
    }
  }
}
