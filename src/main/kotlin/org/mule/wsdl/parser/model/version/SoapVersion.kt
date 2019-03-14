package org.mule.wsdl.parser.model.version

import org.mule.wsdl.parser.model.Constants.SOAP11_ENVELOPE_NS
import org.mule.wsdl.parser.model.Constants.SOAP12_ENCODING_NS
import org.mule.wsdl.parser.model.Constants.SOAP12_ENVELOPE_NS
import org.mule.wsdl.parser.model.Constants.SOAP11_ENCODING_NS
import javax.xml.namespace.QName

enum class SoapVersion {

  SOAP11() {

    private val envelopeQName = QName(SOAP11_ENVELOPE_NS, "Envelope")
    private val bodyQName = QName(SOAP11_ENVELOPE_NS, "Body")
    private val headerQName = QName(SOAP11_ENVELOPE_NS, "Header")

    override fun getEnvelopeQName(): QName = envelopeQName

    override fun getBodyQName(): QName = bodyQName

    override fun getHeaderQName(): QName = headerQName

    override fun getEnvelopeNamespace(): String = SOAP11_ENVELOPE_NS

    override fun getEncodingNamespace(): String = SOAP11_ENCODING_NS

    override fun getContentType(): String = "text/xml"
  },

  SOAP12() {

    private val envelopeQName = QName(SOAP12_ENVELOPE_NS, "Envelope")
    private val bodyQName = QName(SOAP12_ENVELOPE_NS, "Body")
    private val headerQname = QName(SOAP12_ENVELOPE_NS, "Header")

    override fun getEnvelopeQName(): QName = envelopeQName

    override fun getBodyQName(): QName = bodyQName

    override fun getHeaderQName(): QName = headerQname

    override fun getEnvelopeNamespace(): String = SOAP12_ENVELOPE_NS

    override fun getEncodingNamespace(): String = SOAP12_ENCODING_NS

    override fun getContentType(): String = "application/soap+xml"
  };

  abstract fun getEnvelopeQName(): QName

  abstract fun getBodyQName(): QName

  abstract fun getHeaderQName(): QName

  abstract fun getEnvelopeNamespace(): String

  abstract fun getEncodingNamespace(): String

  abstract fun getContentType(): String
}
