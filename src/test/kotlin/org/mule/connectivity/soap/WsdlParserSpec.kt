package org.mule.connectivity.soap

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assert
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.mule.runtime.wsdl.parser.WsdlParser
import org.mule.runtime.wsdl.parser.model.ServiceModel
import java.lang.Thread.*

class WsdlParserSpec : Spek({

  given("a wsdl file") {
    val wsdl = currentThread().contextClassLoader.getResource("wsdl/weather.wsdl")
    val parser = WsdlParser(wsdl.file)

    it("get all services") {
      val services = parser.wsdl.services
      assert.that(services, hasSize(equalTo(1)))
      assert.that(services[0].name, equalTo("GlobalWeather"))
      assert.that(services[0].ports, hasSize(equalTo(4)))
    }
  }
})
