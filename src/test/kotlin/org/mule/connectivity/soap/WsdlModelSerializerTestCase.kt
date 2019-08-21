package org.mule.connectivity.soap

import org.apache.commons.io.IOUtils.toString
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.junit.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.socket.PortFactory
import org.mule.metadata.api.model.ObjectType
import org.mule.wsdl.parser.WsdlParser
import org.mule.wsdl.parser.serializer.WsdlModelSerializer

class WsdlModelSerializerTestCase {

  fun mockServer(response: HttpResponse, freePort: Int): ClientAndServer {
    val server = ClientAndServer.startClientAndServer(freePort)
    val request = HttpRequest.request().withMethod("GET").withPath("/test").withQueryStringParameter("wsdl")
    server.`when`(request, Times.unlimited()).respond(response)
    return server
  }

  @Test
  fun serializeIntoAValidJson() {
    val freePort = PortFactory.findFreePort()

    val content = toString(Thread.currentThread().contextClassLoader.getResourceAsStream("wsdl/simple-service.wsdl"))
    val server = mockServer(HttpResponse.response().withStatusCode(200).withHeaders(Header("Content-Type", "text/xml; charset=utf-8")).withBody(content), freePort)
    val func = { WsdlParser.parse("http://localhost:$freePort/test?wsdl", TestUtils.TestResourceLocator()) }
    val model = func.invoke()
    val serialized = WsdlModelSerializer.serialize(model, true)
    server.stop()
    val expected = toString(Thread.currentThread().contextClassLoader.getResourceAsStream("persistence/serialized-model.json"))
    assertThat(serialized, `is`(expected.replace("##PORT##", freePort.toString()).trim()))
  }

  @Test
  fun deserializeWsdlModelJson() {
    val expected = toString(Thread.currentThread().contextClassLoader.getResourceAsStream("persistence/serialized-model.json"))
    val wsdlModel = WsdlModelSerializer.deserialize(expected.replace("##PORT##", "12120"))
    assertThat(wsdlModel.services, hasSize(1))
    assertThat(wsdlModel.services[0].ports, hasSize(1))
    assertThat(wsdlModel.services[0].ports[0].operations, hasSize(6))
    assertThat(wsdlModel.services[0].ports[0].getOperation("echo").inputType.body, instanceOf(ObjectType::class.java))
  }
}

