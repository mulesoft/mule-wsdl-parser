package org.mule.connectivity.soap

import org.jetbrains.spek.api.Spek

class MultiplePortWsdlSpec : Spek({

//  val PORT_11 = "GlobalWeatherSoap"
//  val PORT_12 = "GlobalWeatherSoap12"
//  val PORT_GET = "GlobalWeatherHttpGet"
//  val PORT_POST = "GlobalWeatherHttpPost"
//
//  val GET_WEATHER = "GetWeather"
//  val GET_CITIES = "GetCitiesByCountry"
//
//  val wsdl = currentThread().contextClassLoader.getResource("wsdl/weather.wsdl")
//  val parsed = WsdlParser.parse(wsdl.file)
//  given("a wsdl file") {
//    it("should have a single service") {
//      assert.that(parsed.services, hasSize(equalTo(1)))
//      assert.that(parsed.services[0].name, equalTo("GlobalWeather"))
//      assert.that(parsed.services[0].ports, hasSize(equalTo(4)))
//    }
//
//    it("should have a service with 4 ports") {
//      val services = parsed.services
//      assert.that(services, hasSize(equalTo(1)))
//      assert.that(services[0].ports.map { it.name }, anyElement(listOf(PORT_11, PORT_12, PORT_GET, PORT_POST)::contains))
//    }
//
//    it("should have ports with 2 operations") {
//      parsed.services[0].ports.forEach({ port ->
//        val operations = port.operations
//        assert.that(operations, hasSize(equalTo(2)))
//        assert.that(operations.map { it.name }, anyElement(listOf(GET_CITIES, GET_WEATHER)::contains))
//      })
//    }
//  }
})
