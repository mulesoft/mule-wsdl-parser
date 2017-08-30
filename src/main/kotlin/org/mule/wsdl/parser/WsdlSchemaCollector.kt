package org.mule.wsdl.parser

import org.mule.metadata.xml.SchemaCollector
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
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.xpath.XPathConstants
import org.w3c.dom.NodeList
import javax.xml.xpath.XPathFactory

internal class WsdlSchemaCollector(definition: Definition) {

  private val schemas: MutableMap<String, Schema> = hashMapOf()
  val parsedSchemas: Map<String, String>

  init {
    collectSchemas(definition)
    parsedSchemas = parseSchemas()
  }

  fun getCollector(): SchemaCollector {
    val collector = SchemaCollector.getInstance()
    parsedSchemas.forEach({ uri, schema -> collector.addSchema(uri, schema) })
    return collector
  }

  private fun parseSchemas(): Map<String, String> {
    val schemaStreams = mutableMapOf<String, String>()
    schemas.forEach({ uri, schema ->
      try {
        schemaStreams.put(uri, nodeToString(schema.element))
      } catch (e: Exception) {
        val message = if (uri.endsWith(".wsdl")) "Schema embedded in wsdl [$uri]" else "Schema [$uri]"
        throw RuntimeException("$message could not be parsed", e)
      }
    })
    return schemaStreams.toMap()
  }

  private fun collectSchemas(definition: Definition) {
    collectFromTypes(definition.types)
    definition.imports.values.forEach { wsdlImport -> if (wsdlImport is Import) collectSchemas(wsdlImport.definition) }
  }

  private fun collectFromTypes(types: Types?) {
    types?.extensibilityElements?.forEach { element -> if (element is Schema) addSchema(element) }
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
    schema.imports.values.forEach { v -> (v as Vector<*>).forEach { e -> if (e is SchemaImport) addSchema(e.referencedSchema) } }
  }

  private fun addIncludedSchemas(schema: Schema) {
    schema.includes.forEach { include -> if (include is SchemaReference) addSchema(include.referencedSchema) }
  }

  private fun nodeToString(node: Node): String {
    try {
      // Remove unwanted whitespaces
      node.normalize()
      val xpath = XPathFactory.newInstance().newXPath()
      val expr = xpath.compile("//text()[normalize-space()='']")
      val nodeList = expr.evaluate(node, XPathConstants.NODESET) as NodeList

      (0..nodeList.length - 1).map { nodeList.item(it) }.forEach { it.parentNode.removeChild(it) }

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
