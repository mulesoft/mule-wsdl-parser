package org.mule.wsdl.parser.locator

import java.io.InputStream

/**
 * This interface acts as an additional layer of indirection between
 * a the WSDL fetching and the WSDL parsing.
 * <p>
 * It enables the retrieval of WSDL and XSD documents that are protected somehow.
 *
 * @since 4.0
 */
interface ResourceLocator {

  /**
   * Given the external document url this method checks if the document can be retrieved by this [ResourceLocator] or not.
   */
  fun handles(url: String): Boolean

  /**
   * Retrieves a document's content.
   */
  fun getResource(url: String): InputStream

}
