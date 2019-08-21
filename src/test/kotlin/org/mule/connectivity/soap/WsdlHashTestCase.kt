package org.mule.connectivity.soap;

import org.junit.Assert.assertNotEquals
import java.io.InputStream;

import org.junit.Test;
import org.mule.wsdl.parser.WsdlParser
import kotlin.test.assertEquals

class WsdlHashTestCase {

  private val FLIGHTS_HASH = "ffffffe22c2220ffffffd14f21ffffffe4bffffffdeffffff8edffff" +
                             "ff9928ffffffa7ffffffb5ffffffea1effffffa946fffffff11b4dff" +
                             "ffff94e70ffffff8bffffffd8ffffffa84cffffffb3ffffff9d"

  private val DOCUMENT_HASH = "ffffff91ffffffa648fffffffefffffff0ffffffaaffffffbdffffff" +
                              "d4ffffff966056ffffff9d12ffffffbc18ffffffc73dffffffe73fff" +
                              "fffebffffff8bffffffab421cffffffc3745fffffffd272781548"
  @Test
  fun sameWsdl() {
    val cl = Thread.currentThread().contextClassLoader
    val wsdl = cl.getResource("wsdl/flights.wsdl");

    val model1 = WsdlParser.parse(wsdl.path, "UTF-8")
    val model2 = WsdlParser.parse(wsdl.path, "UTF-8")
    assertEquals(model1.hash, FLIGHTS_HASH)
    assertEquals(model1.hash, model2.hash)
  }

  @Test
  fun differentWsdl() {
    val cl = Thread.currentThread().contextClassLoader
    val model1 = WsdlParser.parse(cl.getResource("wsdl/flights.wsdl").path, "UTF-8")
    val model2 = WsdlParser.parse(cl.getResource("wsdl/document.wsdl").path, "UTF-8")

    assertNotEquals(model1.hash, model2.hash)
    assertEquals(model1.hash, FLIGHTS_HASH)
    assertEquals(model2.hash, DOCUMENT_HASH)
  }
}
