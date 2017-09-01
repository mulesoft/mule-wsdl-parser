package org.mule.connectivity.soap

import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.mule.wsdl.parser.WsdlParser
import java.lang.Thread.currentThread

class SimpleWsdlSpec : Spek({

  val PORT = "TestPort"
  val SERVICE = "TestService"
  val OPERATIONS = listOf("echo", "echoWithHeaders", "noParam", "noParamWithHeaders", "fail", "echoAccount")

  val wsdl = currentThread().contextClassLoader.getResource("wsdl/simple.wsdl")
  val parsed = WsdlParser.parse(wsdl.file)

  given("a wsdl file") {
    it("should have a single service") {
      assert.that(parsed.services, hasSize(equalTo(1)))
      assert.that(parsed.services[0].name, equalTo(SERVICE))
      assert.that(parsed.services[0].ports, hasSize(equalTo(1)))
    }

    it("should have a service with a single port") {
      val ports = parsed.services[0].ports
      assert.that(ports, hasSize(equalTo(1)))
      assert.that(ports[0].name, equalTo(PORT))
    }

    it("should have ports with 6 operations") {
      val operations = parsed.services[0].ports[0].operations
      assert.that(operations, hasSize(equalTo(6)))
      assert.that(operations.map { it.name }, anyElement(OPERATIONS::contains))
    }
  }
})
