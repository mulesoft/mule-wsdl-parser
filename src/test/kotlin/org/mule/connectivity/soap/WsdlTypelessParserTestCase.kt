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
import org.junit.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.socket.PortFactory
import org.mule.wsdl.parser.WsdlParser
import org.mule.wsdl.parser.WsdlTypelessParser
import org.mule.wsdl.parser.exception.WsdlParsingException
import org.mule.wsdl.parser.model.Version
import org.mule.wsdl.parser.model.WsdlStyle
import java.io.FileInputStream


class WsdlTypelessParserTestCase {

  @Test
  fun shouldBeRPCStyle() {
    val wsdl = WsdlTypelessParser.parse(TestUtils.getResourcePath("wsdl/rpc.wsdl"))
    wsdl.style shouldBe WsdlStyle.RPC
  }

  @Test
  fun shouldHaveAddress() {
    val wsdl = WsdlTypelessParser.parse(TestUtils.getResourcePath("wsdl/relative-address.wsdl"))
    val address = wsdl.services[0].ports[0].address

    assertThat(address.toString(), `is`("/disweb/1.0/spring-ws"))
  }

  @Test
  fun shouldHaveASingleServiceWithASinglePort() {
    val wsdl = WsdlTypelessParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))

    assertThat(wsdl.services, Matchers.hasSize(1))
    assertThat(wsdl.services[0].name, `is`("TestService"))
    assertThat(wsdl.services[0].ports, Matchers.hasSize(1))
    assertThat(wsdl.services[0].ports[0].name, `is`("TestPort"))
  }

}


