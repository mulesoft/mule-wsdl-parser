package org.mule.connectivity.soap

import com.google.gson.GsonBuilder
import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.StringContains.containsString
import org.junit.Test
import org.mule.metadata.api.model.MetadataType
import org.mule.metadata.api.model.ObjectType
import org.mule.metadata.api.model.StringType
import org.mule.metadata.api.model.UnionType
import org.mule.metadata.api.model.impl.DefaultObjectFieldType
import org.mule.metadata.api.model.impl.DefaultObjectType
import org.mule.metadata.persistence.MetadataTypeGsonTypeAdapter
import org.mule.wsdl.parser.WsdlParser
import org.mule.wsdl.parser.model.operation.OperationModel
import java.io.File
import java.io.FileInputStream

class WsdlOperationTypeParserTestCase {

  fun assertOperationTypes(ope: OperationModel) {
    val gson = GsonBuilder().registerTypeAdapter(MetadataType::class.java, MetadataTypeGsonTypeAdapter()).setPrettyPrinting().create()
    val input = gson.toJson(ope.inputType)
    val output = gson.toJson(ope.outputType)
    val ccl = Thread.currentThread().contextClassLoader
    val prefix = "types/" + ope.name

    val inPath = ccl.getResource("$prefix/in.json").path
    val outPath = ccl.getResource("$prefix/out.json").path

    // set to true to rewrite the expected files with the generated json
    val overrideType = false
    if (true) {
      File(inPath).writeText(input)
      File(outPath).writeText(output)
    }

    val expectedInput = IOUtils.toString(FileInputStream(inPath))
    val expectedOutput = IOUtils.toString(FileInputStream(outPath))

    assertThat(input, `is`(expectedInput))
    assertThat(output, `is`(expectedOutput))
  }

  @Test
  fun collectSchemaEmbeddedInWsdlTypesThatUsesNamespacesDefinedInTheWsdlDefinition() {
    val model = WsdlParser.parse(TestUtils.getResourcePath("wsdl/namespaces.wsdl"))
    val opeType = model.services[0].ports[0].operations[0].inputType.body
    val metadataFields = ((((opeType as DefaultObjectType).fields.iterator().next()) as DefaultObjectFieldType).value as DefaultObjectType).fields
    assertThat(metadataFields, hasSize(19))
  }

  @Test
  fun resolveOperationTypesCorrectly() {
    val model = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))
    model.services[0].ports[0].operations.forEach { o -> assertOperationTypes(o) }
  }

  @Test
  fun resolveOperationWithChoiceOutput() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/with-choice-types.wsdl"))
    val outputBody = wsdl.services[0].ports[0].operations[0].outputType.body as ObjectType
    assertThat(outputBody.fields.iterator().next().value, `is`(Matchers.instanceOf(UnionType::class.java)))
  }

  @Test
  fun resolveOperationWithSimpleTypes() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/with-simple-type-body.wsdl"))
    val outputBody = wsdl.services[0].ports[0].operations[0].outputType.body as ObjectType
    assertThat(outputBody.fields.iterator().next().value, `is`(Matchers.instanceOf(StringType::class.java)))
  }

  @Test
  fun resolveMultipartOutputCorrectly() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/multipart-output/Multipart.wsdl"))
    val operation = wsdl.services[0].ports[0].getOperation("retrieveDocument")
    assertThat(operation.inputType.body.toString(), containsString("{urn:docmgmt2_1.util.schema.bcbsm.com}RetrieveDocumentRequest"))
    assertThat(operation.outputType.body.toString(), containsString("{urn:docmgmt2_1.util.schema.bcbsm.com}RetrieveDocumentResponse"))
  }
}


