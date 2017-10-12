package org.mule.connectivity.soap

import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.hamcrest.Matchers.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.mockserver.integration.ClientAndServer
import org.mule.wsdl.parser.WsdlParser
import org.mule.wsdl.parser.exception.WsdlParsingException
import org.mule.wsdl.parser.model.WsdlStyle
import org.mockserver.matchers.Times
import org.mockserver.model.Header
import org.mockserver.socket.PortFactory
import java.io.FileInputStream
import org.amshove.kluent.*


class WsdlParserSpec : Spek({

  given("a wsdl file") {
    describe("it's style") {
      it("should be of doc literal defined by the operations") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/document.wsdl"))
        wsdl.style shouldBe WsdlStyle.DOC_LITERAL
      }

      it("should be of doc literal style because there is no style specification") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/no-style-defined.wsdl"))
        wsdl.style shouldBe WsdlStyle.DOC_LITERAL
      }

      it("should be of doc literal style defined in the binding") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/rpc.wsdl"))
        wsdl.style shouldBe WsdlStyle.RPC
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

      describe("a http protected wsdl") {

        val freePort = PortFactory.findFreePort()
        val wsdlContent = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("wsdl/document.wsdl")))

        fun createServer(): ClientAndServer? {
          val server = ClientAndServer.startClientAndServer(freePort)
          server.`when`(request()
              .withMethod("GET")
              .withPath("/test")
              .withQueryStringParameter("wsdl"),
              Times.once())
              .callback({ req ->
                if (req.containsHeader("Auth")) {
                  response()
                      .withStatusCode(200)
                      .withHeaders(Header("Content-Type", "text/xml; charset=utf-8"))
                      .withBody(wsdlContent)
                } else {
                  response().withStatusCode(401)
                }
              })
          return server
        }

//        it("should fail to access the protected wsdl") {
//          val server = createServer()
//          val func = { val wsdl = WsdlParser.parse("http://localhost:$freePort/test?wsdl") }
//          func.invoke()
//          server?.stop(true)
//        }

        it("should access the protected wsdl") {
          val server = createServer()
          val func = { val wsdl = WsdlParser.parse("http://localhost:$freePort/test?wsdl", TestUtils.TestResourceLocator()) }
          func.invoke()
          server?.stop(true)
        }

        on("an invalid location") {
          it("should fail gracefully") {
            val msg = "Error processing WSDL file [invalid/location]: Unable to locate document at 'invalid/location'."
            { val res = WsdlParser.parse("invalid/location") } shouldThrowTheException WsdlParsingException::class withMessage msg
          }
        }
      }
    }
  }
})



