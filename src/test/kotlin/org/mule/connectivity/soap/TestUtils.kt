package org.mule.connectivity.soap

import org.apache.commons.io.IOUtils
import org.custommonkey.xmlunit.XMLUnit
import org.custommonkey.xmlunit.XMLUnit.compareXML
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.mockserver.client.server.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mule.wsdl.parser.locator.ResourceLocator
import org.xml.sax.InputSource
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import com.turbomanage.httpclient.ParameterMap
import com.turbomanage.httpclient.BasicHttpClient



object TestUtils {

  fun getResourcePath(name: String) = Thread.currentThread().contextClassLoader.getResource(name).path!!

  fun assertSimilarXml(expected: String, result: InputStream) {
    assertSimilarXml(expected, IOUtils.toString(result))
  }

  fun assertSimilarXml(expected: String, result: String) {
    XMLUnit.setIgnoreWhitespace(true)
    val diff = compareXML(result, expected)
    if (!diff.similar()) {
      println("Expected xml is:\n")
      println(prettyPrint(expected))
      println("########################################\n")
      println("But got:\n")
      println(prettyPrint(result))
    }
    MatcherAssert.assertThat(diff.similar(), Is.`is`(true))
  }

  private fun prettyPrint(a: String): String {
    val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val `is` = InputSource()
    `is`.characterStream = StringReader(a)
    val doc = db.parse(`is`)
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    // initialize StreamResult with File object to save to file
    val result = StreamResult(StringWriter())
    val source = DOMSource(doc)
    transformer.transform(source, result)
    return result.writer.toString()
  }

  class TestResourceLocator: ResourceLocator {

    override fun handles(url: String): Boolean = url.startsWith("http")

    override fun getResource(url: String): InputStream {
      val httpClient = BasicHttpClient(url)
      httpClient.addHeader("Auth", "yay")
      httpClient.setConnectionTimeout(2000)
      return httpClient.get("", httpClient.newParams()).bodyAsString.byteInputStream()
    }
  }
}

