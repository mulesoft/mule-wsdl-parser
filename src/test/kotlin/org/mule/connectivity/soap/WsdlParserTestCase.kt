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
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.junit.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.socket.PortFactory
import org.mule.connectivity.soap.TestUtils.getResourcePath
import org.mule.wsdl.parser.WsdlParser
import org.mule.wsdl.parser.exception.OperationNotFoundException
import org.mule.wsdl.parser.exception.WsdlGettingException
import org.mule.wsdl.parser.exception.WsdlParsingException
import org.mule.wsdl.parser.model.WsdlStyle
import org.mule.wsdl.parser.model.version.SoapVersion
import java.io.File
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
    wsdl.services[0].ports[0].binding!!.version shouldBe SoapVersion.SOAP11
  }

  @Test
  fun shouldBeVersion12() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/hello-world-soap12.wsdl"))
    wsdl.services[0].ports[0].binding!!.version shouldBe SoapVersion.SOAP12
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
  fun findSOAP12Headers() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-soap12-service.wsdl"))
    val messagePart = wsdl.services[0].ports[0].getOperationsMap().get("echoWithHeaders")?.inputType?.message?.parts?.findLast { m -> m.name == "headerIn" }

    assertThat(messagePart?.isHeader, `is`(true))
  }

  @Test
  fun findSOAP11Headers() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))
    val messagePart = wsdl.services[0].ports[0].getOperationsMap().get("echoWithHeaders")?.inputType?.message?.parts?.findLast { m -> m.name == "headerIn" }

    assertThat(messagePart?.isHeader, `is`(true))
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
    val msg = "Error Getting the resource [$resource]: Server returned HTTP response code: 401 for URL: $resource"
    func shouldThrowTheException WsdlGettingException::class withMessage msg
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

  @Test
  fun shouldParseWSDLWithMissingOutputBinding() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/binding-missing-output.wsdl"))
    val ops = wsdl.services[0].ports[0].operations
    assertThat(ops, hasSize(1))
    assertThat(ops.map { it.name }, hasItems("echo"))
  }

  @Test
  fun shouldParseWSDLWithMissingInputBinding() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/binding-missing-input.wsdl"))
    val ops = wsdl.services[0].ports[0].operations
    assertThat(ops, hasSize(1))
    assertThat(ops.map { it.name }, hasItems("echo"))
  }

  @Test
  fun shouldParseWSDLWithPortTypeMissingOperationInput() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/porttype-missing-operation-input.wsdl"))
    val ops = wsdl.services[0].ports[0].operations
    assertThat(ops, hasSize(1))
    assertThat(ops.map { it.name }, hasItems("echo"))
  }

  @Test
  fun shouldParseWSDLWithPortTypeMissingOperationOutput() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/porttype-missing-operation-output.wsdl"))
    val ops = wsdl.services[0].ports[0].operations
    assertThat(ops, hasSize(1))
    assertThat(ops.map { it.name }, hasItems("echo"))
  }

  @Test
  fun ignoreHTTPBindings() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/http-binding.wsdl"))
    val ports = wsdl.services[0].ports
    assertThat(ports, hasSize(2))
    assertThat(ports.map { it.name }, not(hasItems("TestWebServiceHttpGet")))
  }

  @Test
  fun parseCircularDependent() {
    val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/circular/service-11-A.wsdl"))
    val services = wsdl.services
    assertThat(services, hasSize(1))
    assertThat(services.map { it.name }, hasItems("TestService"))
  }

  @Test
  fun shouldParserWSDLGivenHTTPLocation() {
    val freePort = PortFactory.findFreePort()

    val wsdlPath = getResourcePath("wsdl/local-imports/main.wsdl")
    val importPath = getResourcePath("wsdl/local-imports/types.xsd")
    val wsdlString = File(wsdlPath).readText(Charsets.UTF_8)
    val importString =  File(importPath).readText(Charsets.UTF_8).replace("types.xsd", "http://localhost:$freePort/types.xsd")

    val server = mockServer(response().withBody(wsdlString), freePort)
    server.`when`(request().withMethod("GET").withPath("/types.xsd"), Times.once()).respond(response().withBody(importString))

    try {
      val wsdl = WsdlParser.parse("http://localhost:$freePort/test?wsdl")
      val echo = wsdl.getOperation("PingBulk")
      assertThat(echo, `is`(notNullValue()))
    } finally {
      server.stop(true)
    }
  }

  fun mockServer(response: HttpResponse, freePort: Int): ClientAndServer {
    val server = ClientAndServer.startClientAndServer(freePort)
    server.`when`(request().withMethod("GET").withPath("/test").withQueryStringParameter("wsdl"), Times.once()).respond(response)
    return server
  }

}


