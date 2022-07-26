package org.mule.connectivity.soap

import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Ignore
import org.junit.Test
import org.mule.wsdl.parser.WsdlSchemasCollector
import java.io.FileInputStream
import javax.wsdl.Definition
import javax.wsdl.factory.WSDLFactory

class WsdlSchemasCollectorTestCase {

  fun testDefinition(wsdl: String): Definition {
    val factory = WSDLFactory.newInstance()
    val wsdlReader = factory.newWSDLReader()
    wsdlReader.setFeature("javax.wsdl.verbose", false)
    wsdlReader.setFeature("javax.wsdl.importDocuments", true)
    return wsdlReader.readWSDL(TestUtils.getResourcePath(wsdl))
  }

  @Test
  fun shouldCollectSchemaEmbeddedInTheWsdlTypes() {
    val schemas = WsdlSchemasCollector(testDefinition("wsdl/simple-service.wsdl")).collector().collect()
    assertThat(schemas.values, hasSize(1))
    val expected = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("schemas/simple-service-types.xsd")))
    TestUtils.assertSimilarXml(expected, schemas.values.iterator().next())
  }

  @Test
  fun shouldCollectSchemasThatContainsRecursiveReferences() {
    val schemas = WsdlSchemasCollector(testDefinition("wsdl/recursive/main.wsdl")).collector().collect()
    assertThat(schemas.values, hasSize(6))
  }

  @Test
  @Ignore("Ignored to fix NexusIQ violation and update dependency (W-11391319) . Related to issue W-11379502 to fix")
  fun shouldCollectSchemaWithNoLocation() {
    val schemas = WsdlSchemasCollector(testDefinition("wsdl/no-schema-location/test.wsdl")).collector().collect()
    assertThat(schemas.entries, hasSize(4))
  }

  @Test
  fun shouldCollectMultipleSchemasEmbeddedInTheWsdlTypes() {
    val schemas = WsdlSchemasCollector(testDefinition("wsdl/types-multiple-schema.wsdl")).collector().collect()
    assertThat(schemas.values, hasSize(2))
  }

  @Test
  fun shouldCollectComplexEmbeddedSchema() {
    val schemas = WsdlSchemasCollector(testDefinition("wsdl/with-complex-embedded-schema.wsdl")).collector().collect()
    assertThat(schemas.values, hasSize(1))
    val expected = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("schemas/complex-embedded-schema.xsd")))
    TestUtils.assertSimilarXml(expected, schemas.values.iterator().next())
  }

  @Test
  fun shouldCollectSchemaEmbeddedInInportedWsdlWithMultipleImports() {
    val schemas = WsdlSchemasCollector(testDefinition("wsdl/multiple-imports-as-value/multiple-imports-base.wsdl")).collector().collect()
    assertThat(schemas.values, hasSize(1))
    val expected = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("schemas/multiple-import-values.xsd")))
    TestUtils.assertSimilarXml(expected, schemas.values.iterator().next())
  }

  @Test
  fun shouldCollectSchemasWithoutID() {
    val schemas = WsdlSchemasCollector(testDefinition("wsdl/multiple-schemas-in-types-without-key/GLDailyRatesOutProvABCSImpl_PSWithLocalXSD.wsdl")).collector().collect()
    assertThat(schemas.values, hasSize(10))
  }
}

