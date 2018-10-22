package org.mule.connectivity.soap

import org.amshove.kluent.shouldBe
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.junit.Test
import org.mule.wsdl.parser.WsdlTypelessParser
import org.mule.wsdl.parser.model.WsdlStyle


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


