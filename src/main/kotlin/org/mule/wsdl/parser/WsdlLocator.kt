package org.mule.wsdl.parser

import org.apache.cxf.resource.URIResolver
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

import javax.wsdl.xml.WSDLLocator

import org.apache.cxf.wsdl11.CatalogWSDLLocator
import org.mule.wsdl.parser.exception.WsdlParsingException
import org.mule.wsdl.parser.locator.GlobalResourceLocator
import org.mule.wsdl.parser.locator.ResourceLocator
import org.xml.sax.InputSource

/**
 * [WSDLLocator] implementation that enables the retrieval of WSDL document and associated files
 * that are protected using a [ResourceLocator] instance.
 *
 * If the [ResourceLocator] cannot retrieve a document then a delegate [CatalogWSDLLocator]
 * will try to retrieve it.
 */
internal class WsdlLocator(private val wsdlLocation: String, private val resourceLocator: ResourceLocator) : WSDLLocator {

  private val delegateLocator: ResourceLocator
  private val streams = ArrayList<InputStream>()

  /**
   * Mutable field, gets updated each time a new import is found
   */
  private var latestImportUri: String = wsdlLocation

  init {
    this.delegateLocator = GlobalResourceLocator()
  }

  /**
   * Returns an InputSource "pointed at" the base document.
   *
   *
   * If the wsdl location can be fetched by the [ResourceLocator] it is consumed by it otherwise we
   * delegate the search to the delegate cxf [CatalogWSDLLocator].
   */
  override fun getBaseInputSource(): InputSource? {
      return getInputSource(wsdlLocation)
  }

  /**
   * Returns an InputSource "pointed at" an imported wsdl document.
   *
   *
   * If the imported resource can be fetched by the [ResourceLocator] then it gets fetched, otherwise
   * the fetching is delegated to the [CatalogWSDLLocator].
   */
  override fun getImportInputSource(parentLocation: String, importLocation: String): InputSource {
    val resolved = URIResolver(parentLocation, importLocation).uri.toURL().toString()
    latestImportUri = resolved
    return getInputSource(resolved)
  }

  /**
   * @return an URI representing the location of the base document.
   */
  override fun getBaseURI(): String {
    return wsdlLocation
  }

  /**
   * @return an URI representing the location of the last import document
   * * to be resolved. This is used in resolving nested imports where an
   * * import location is relative to the parent document.
   */
  override fun getLatestImportURI(): String {
    return latestImportUri
  }

  /**
   * Releases all the [InputStream]s opened to parse the wsdl and resource files.
   */
  override fun close() {
    streams.forEach { stream ->
      try {
        stream.close()
      } catch (e: IOException) {
      }
    }
  }

  private fun getInputSource(url: String): InputSource {
    try {
      val handler = if (resourceLocator.handles(wsdlLocation)) resourceLocator else delegateLocator
      val resultStream = handler.getResource(url)
      streams.add(resultStream)
      return InputSource(resultStream)
    } catch (e: Exception) {
      throw WsdlParsingException("Error fetching the resource [" + url + "]: " + e.message, e)
    }
  }
}
