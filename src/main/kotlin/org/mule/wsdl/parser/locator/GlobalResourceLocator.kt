package org.mule.wsdl.parser.locator

import org.apache.cxf.resource.URIResolver
import java.io.InputStream

class GlobalResourceLocator : ResourceLocator {
  override fun getResource(url: String): InputStream {
    val resolver = URIResolver(url)
    if (resolver.isResolved)
      return resolver.inputStream

    throw RuntimeException("Cannot resolve URL: [$url]")
  }

  override fun handles(url: String): Boolean {
    return true
  }
}
