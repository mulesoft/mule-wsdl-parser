package org.mule.wsdl.parser.locator

import java.io.InputStream

class NullResourceLocator : ResourceLocator {
  override fun getResource(url: String): InputStream = "".byteInputStream()
  override fun handles(url: String): Boolean = false
}
