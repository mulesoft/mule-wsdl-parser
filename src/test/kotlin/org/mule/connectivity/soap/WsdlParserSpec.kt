package org.mule.connectivity.soap

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrowTheException
import org.amshove.kluent.withMessage
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
import org.mockserver.model.HttpResponse
import org.mockserver.socket.PortFactory
import java.io.FileInputStream


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

    describe("a multipart output") {
      it("should be correctly parsed") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/multipart-output/Multipart.wsdl"))
        val operation = wsdl.services[0].ports[0].getOperation("retrieveDocument")
        val bodyPart = operation.getOutputBodyPart()
        bodyPart.isPresent shouldEqual true
        bodyPart.get().name shouldEqual "response"
      }
    }

    describe("a WSDL with an operation that") {
      it("does not have a soap action defined") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/flights.wsdl"))
        val soapAction = wsdl.services[0].ports[0].operations[0].soapAction
        soapAction.isPresent shouldBe false
      }

      it("has a soap action defined") {
        val wsdl = WsdlParser.parse(TestUtils.getResourcePath("wsdl/simple-service.wsdl"))
        val soapAction = wsdl.services[0].ports[0].operations[0].soapAction
        soapAction.isPresent shouldBe true
        soapAction.get() shouldEqual "echoOperation"
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

        it("should have fail operation with 1 fault") {
          val failOp = wsdl.services[0].ports[0].getOperation("fail")
          failOp.name shouldEqual "fail"
          assertThat(failOp.getFaults(), hasSize(1))
          assertThat(failOp.getFault("EchoException").getName(), `is`("EchoException"))
        }
      }

      describe("a http protected wsdl") {
        val freePort = PortFactory.findFreePort()

        fun mockServer(response: HttpResponse): ClientAndServer {
          val server = ClientAndServer.startClientAndServer(freePort)
          server.`when`(request().withMethod("GET").withPath("/test").withQueryStringParameter("wsdl"), Times.once()).respond(response)
          return server
        }

        it("should fail to access the protected wsdl") {
          val server = mockServer(response().withStatusCode(401))
          val func = { val wsdl = WsdlParser.parse("http://localhost:$freePort/test?wsdl") }
          val msg = "Error processing WSDL file [http://localhost:$freePort/test?wsdl]: Unable to locate document at 'http://localhost:$freePort/test?wsdl'."
          func shouldThrowTheException WsdlParsingException::class withMessage msg
          server.stop(true)
        }

        it("should access the protected wsdl") {
          val wsdlContent = IOUtils.toString(FileInputStream(TestUtils.getResourcePath("wsdl/document.wsdl")))
          val server = mockServer(response().withStatusCode(200).withHeaders(Header("Content-Type", "text/xml; charset=utf-8")).withBody(wsdlContent))
          val func = { val wsdl = WsdlParser.parse("http://localhost:$freePort/test?wsdl", TestUtils.TestResourceLocator()) }
          func.invoke()
          server.stop()
        }

        on("an invalid location") {
          it("should fail gracefully") {
            val msg = "Error processing WSDL file [invalid/location]: Unable to locate document at 'invalid/location'."
            val func = { val res = WsdlParser.parse("invalid/location") }
            func shouldThrowTheException WsdlParsingException::class withMessage msg
          }
        }
      }
    }
  }
})


