package org.mule.connectivity.soap

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrowTheException
import org.amshove.kluent.withMessage
import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.notNullValue
import org.junit.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.socket.PortFactory
import org.mule.metadata.api.model.ObjectType
import org.mule.metadata.api.model.UnionType
import org.mule.wsdl.parser.WsdlParser
import org.mule.wsdl.parser.exception.OperationNotFoundException
import org.mule.wsdl.parser.exception.WsdlParsingException
import org.mule.wsdl.parser.model.Version
import org.mule.wsdl.parser.model.WsdlStyle
import java.io.FileInputStream


class WsdlParserTestCase {

  @Test
  fun shouldBeDocLiteralDefiniedInTheOperations() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/document.wsdl"))
    wsdl.style shouldBe WsdlStyle.DOC_LITERAL
  }

  @Test
  fun shouldBeDocLiteralByDefault() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/no-style-defined.wsdl"))
    wsdl.style shouldBe WsdlStyle.DOC_LITERAL
  }

  @Test
  fun shouldBeRPCStyle() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/rpc.wsdl"))
    wsdl.style shouldBe WsdlStyle.RPC
  }

  @Test
  fun shouldBeVersion11() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))
    wsdl.services[0].ports[0].binding!!.version shouldBe Version.V1_1
  }

  @Test
  fun shouldBeVersion12() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/hello-world-soap12.wsdl"))
    wsdl.services[0].ports[0].binding!!.version shouldBe Version.V1_2
  }

  @Test
  fun shouldHaveAnOperationWithoutAction() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/flights.wsdl"))
    val soapAction = wsdl.services[0].ports[0].operations[0].soapAction
    soapAction!! shouldBe ""
  }

  @Test
  fun shouldHaveAnOperationWithSoapAction() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))
    val soapAction = wsdl.services[0].ports[0].operations[0].soapAction
    soapAction!! shouldEqual "echoOperation"
  }

  @Test
  fun shouldHaveASingleServiceWithASinglePort() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))

    assertThat(wsdl.services, Matchers.hasSize(1))
    assertThat(wsdl.services[0].name, `is`("TestService"))
    assertThat(wsdl.services[0].ports, Matchers.hasSize(1))
    assertThat(wsdl.services[0].ports[0].name, `is`("TestPort"))
  }

  @Test
  fun wsdlWithUnionTypeOutput() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/with-choice-types.wsdl"))
    val outputBody = wsdl.services[0].ports[0].operations[0].outputType.body as ObjectType
    assertThat(outputBody.fields.iterator().next().value, `is`(instanceOf(UnionType::class.java)))
  }

  @Test
  fun shouldHaveAnOperationWithBindingInImportedWsdl() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/recursive/main.wsdl"))
    val ops = wsdl.services[0].ports[0].operations
    assertThat(ops, hasSize(1))
    assertThat(ops.map { it.name }, hasItems("Authenticate"))
  }

  @Test
  fun shouldHave6OperationsDefined() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))
    val ops = wsdl.services[0].ports[0].operations
    assertThat(ops, hasSize(6))
    assertThat(ops.map { it.name }, hasItems("echo", "noParams", "echoAccount", "echoWithHeaders", "noParamsWithHeader", "fail"))
  }

  @Test
  fun findOperationFromWsdlRoot() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))
    val echo = wsdl.getOperation("echo")
    assertThat(echo, `is`(notNullValue()))
  }

  @Test
  fun multipleOperationsSameName() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/currency-converter.wsdl"))

    val func = { val res = wsdl.getOperation("ConversionRate") }
    func shouldThrowTheException OperationNotFoundException::class withMessage "Multiple operations [ConversionRate] found, the operation may be defined in multiple ports"
  }

  @Test
  fun operationDefinesAFault() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))
    val failOp = wsdl.services[0].ports[0].getOperation("fail")
    failOp.name shouldEqual "fail"
    assertThat(failOp.faults, hasSize(1))
    assertThat(failOp.getFault("EchoException").get().name, `is`("EchoException"))
  }

  fun mockServer(response: HttpResponse, freePort: Int): ClientAndServer {
    val server = ClientAndServer.startClientAndServer(freePort)
    server.`when`(request().withMethod("GET").withPath("/test").withQueryStringParameter("wsdl"), Times.once()).respond(response)
    return server
  }

  @Test
  fun shouldFailWhenFetchingProtectedWsdl() {
    val freePort = PortFactory.findFreePort()
    val server = mockServer(response().withStatusCode(401), freePort)
    val func = {
      try {
        val wsdl = WsdlParser.parse("http://localhost:$freePort/test?wsdl")
      } finally {
        server.stop(true)
      }
    }
    val resource = "http://localhost:$freePort/test?wsdl"
    val msg = "Error fetching the resource [$resource]: Server returned HTTP response code: 401 for URL: $resource"
    func shouldThrowTheException WsdlParsingException::class withMessage msg
  }

  @Test
  fun shouldParseProtectedWsdl() {
    val freePort = PortFactory.findFreePort()
    val wsdlContent = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("wsdl/document.wsdl")))
    val server = mockServer(response().withStatusCode(200).withHeaders(Header("Content-Type", "text/xml; charset=utf-8")).withBody(wsdlContent), freePort)
    val func = { val wsdl = WsdlParser.parse("http://localhost:$freePort/test?wsdl", TestUtils.TestResourceLocator()) }
    func.invoke()
    server.stop()
  }

  @Test
  fun shouldFailWhenParsingInvalidLocation() {
    val msg = "Error fetching the resource [invalid/location]: Cannot resolve URL: [invalid/location]"
    val func = { val res = WsdlParser.parse("invalid/location") }
    func shouldThrowTheException WsdlParsingException::class withMessage msg
  }
}


