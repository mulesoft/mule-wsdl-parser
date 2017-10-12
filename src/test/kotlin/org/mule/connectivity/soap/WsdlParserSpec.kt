package org.mule.connectivity.soap

import com.sun.xml.internal.ws.api.server.ServiceDefinition
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.mule.wsdl.parser.WsdlParser
import org.mule.wsdl.parser.model.WsdlStyle
import java.io.File
import java.lang.Thread.currentThread
import java.net.URISyntaxException

class WsdlParserSpec : Spek({

  given("a wsdl file") {
    describe("it's style") {
      it("should be of doc literal defined by the operations") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/document.wsdl"))
        assertThat(wsdl.style, `is`(WsdlStyle.DOC_LITERAL))
      }

      it("should be of doc literal style because there is no style specification") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/no-style-defined.wsdl"))
        assertThat(wsdl.style, `is`(WsdlStyle.DOC_LITERAL))
      }

      it("should be of doc literal style defined in the binding") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/rpc.wsdl"))
        assertThat(wsdl.style, `is`(WsdlStyle.RPC))
      }
    }

    describe("it's structure") {
      on("a simple wsdl") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))

        it("should have a single service with a single port") {
          assertThat(wsdl.services, Matchers.hasSize(1))
          assertThat(wsdl.services[0].name, `is`("TestService"))
          assertThat(wsdl.services[0].ports, Matchers.hasSize(1))
          assertThat(wsdl.services[0].ports[0].name, `is`("TestPort"))
        }

        it("should have ports with 6 operations") {
          val ops = wsdl.services[0].ports[0].operations
          assertThat(ops, hasSize(6))
          assertThat(ops.map { it.name }, hasItems("echo", "noParams", "echoAccount", "echoWithHeaders", "noParamsWithHeader", "fail"))
        }
      }

      on("a http protected wsdl") {
        it("should fail to access the protected wsdl") {
          //    expectedException.expect(InvalidWsdlException::class.java)
          //    expectedException.expectMessage("faultCode=OTHER_ERROR: Unable to locate document at")
          //    val server = BasicAuthHttpServer(port.getNumber(), null, null, Soap11Service())
          //    val resourceLocation = server.getDefaultAddress() + "?wsdl"
          //    ServiceDefinition(resourceLocation, "TestService", "TestPort")
          //    server.stop()
        }

        it("should access the protected wsdl") {
//          val server = BasicAuthHttpServer(port.getNumber(), null, null, Soap11Service())
          //    val resourceLocator = HttpBasicAuthResourceLocator()
          //    resourceLocator.start()
          //    val resourceLocation = server.getDefaultAddress() + "?wsdl"
          //    val definition = ServiceDefinition(resourceLocation, "TestService", "TestPort", resourceLocator)
          //    resourceLocator.stop()
          //    server.stop()
          //    assertThat(definition.isDocumentStyle(), `is`(true))
        }
      }

      on("an invalid location") {
        it("should fail gracefully") {
          val wsdl = WsdlParser.parse("invalid/location")
        }
      }
    }

  }
})


