package org.mule.wsdl.parser

import org.mule.metadata.xml.api.SchemaCollector
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.StringWriter
import java.util.*
import javax.wsdl.Definition
import javax.wsdl.Import
import javax.wsdl.Types
import javax.wsdl.extensions.schema.Schema
import javax.wsdl.extensions.schema.SchemaImport
import javax.wsdl.extensions.schema.SchemaReference
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

import kotlin.collections.HashMap


/**
 * The purpose of this class is to find all the schema URLs, both local or remote, for a given WSDL definition. This includes
 * imports and includes in the WSDL file and recursively in each schema found.
 *
 * @since 1.0
 */
@SuppressWarnings("unchecked")
class WsdlSchemasCollector(private val definition: Definition) {

  private val schemas = HashMap<String, Schema>()

  companion object {
    val TARGET_NS = "targetNamespace"
  }

  fun collector() : SchemaCollector {
    val collector = SchemaCollector.getInstance()
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
      if (import is Import) {
        collectSchemas(import.definition)
      }
    }
  }

  private fun collectFromTypes(types: Types?) {
    types?.extensibilityElements?.forEach { element ->
      if (element is Schema) {
        addSchema(element.element.getAttribute(TARGET_NS) ?: element.documentBaseURI, element)
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
      // Remove unwanted whitespaces
      node.normalize()
      val xpath = XPathFactory.newInstance().newXPath()
      val expr = xpath.compile("//text()[normalize-space()='']")
      val nodeList = expr.evaluate(node, XPathConstants.NODESET) as NodeList

      (0..nodeList.length - 1)
          .map { nodeList.item(it) }
          .forEach { it.parentNode.removeChild(it) }

      // Create and setup transformer
      val transformer = TransformerFactory.newInstance().newTransformer()
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

      // Turn the node into a string
      val writer = StringWriter()
      transformer.transform(DOMSource(node), StreamResult(writer))
      return writer.toString()
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }
}
