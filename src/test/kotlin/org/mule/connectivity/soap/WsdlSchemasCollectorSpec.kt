package org.mule.connectivity.soap

import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.mule.wsdl.parser.WsdlParser
import java.io.FileInputStream

class WsdlSchemasCollectorSpec : Spek({

  given("a wsdl file") {
    it("should collect the schema embedded in the wsdl types tag") {
      val schemas = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl")).collectSchemas()
      assertThat(schemas.values, hasSize(1))
      val expected = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("schemas/simple-service-types.xsd")))
      TestUtils.assertSimilarXml(expected, schemas.values.iterator().next())
    }

    it("should collect all the local referenced schemas that contains recursive references") {
      val schemas = WsdlParser.parse(TestUtils.getResourcePath("wsdl/recursive/main.wsdl")).collectSchemas()
      assertThat(schemas.values, hasSize(7))
    }

    it("has an schema that does not specify a location") {
      val schemas = WsdlParser.parse(TestUtils.getResourcePath("wsdl/no-schema-location/test.wsdl")).collectSchemas()
      assertThat(schemas.entries, hasSize(4))
    }

    it("should collector multiples schemas embedded in the wsdl types tag") {
      val schemas = WsdlParser.parse(TestUtils.getResourcePath("wsdl/types-multiple-schema.wsdl")).collectSchemas()
      assertThat(schemas.values, hasSize(2))
    }

    it("should collect the schema embedded in the wsdl types tag and assert the extracted content") {
      val schemas = WsdlParser.parse(TestUtils.getResourcePath("wsdl/with-complex-embedded-schema.wsdl")).collectSchemas()
      assertThat(schemas.values, hasSize(1))
      val expected = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("schemas/complex-embedded-schema.xsd")))
      TestUtils.assertSimilarXml(expected, schemas.values.iterator().next())
    }

    it("should collect the schema embedded in an imported wsdl with multiple import values") {
      val parsed = WsdlParser.parse(TestUtils.getResourcePath("wsdl/multiple-imports-as-value/multiple-imports-base.wsdl"))
      val schemas = parsed.collectSchemas()
      assertThat(schemas.values, hasSize(1))
      val expected = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("schemas/multiple-import-values.xsd")))
      TestUtils.assertSimilarXml(expected, schemas.values.iterator().next())
    }
    it("should be correctly parsed") {
      val schemas = WsdlParser.parse(TestUtils.getResourcePath("wsdl/multiple-schemas-in-types-without-key")).collectSchemas()
      assertThat(schemas.values, hasSize(10))
    }
  }
})

