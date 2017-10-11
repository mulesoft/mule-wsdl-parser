package org.mule.wsdl.parser.model.operation

import org.mule.wsdl.parser.model.FaultModel
import org.mule.wsdl.parser.model.NamedModel
import org.mule.wsdl.parser.model.WsdlStyle
import org.mule.wsdl.parser.model.WsdlStyleFinder
import java.util.*
import javax.wsdl.*
import javax.wsdl.extensions.ElementExtensible
import javax.wsdl.extensions.soap.SOAPBody
import javax.wsdl.extensions.soap.SOAPHeader
import javax.wsdl.extensions.soap.SOAPOperation
import javax.wsdl.extensions.soap12.SOAP12Body
import javax.wsdl.extensions.soap12.SOAP12Header
import javax.wsdl.extensions.soap12.SOAP12Operation

class OperationModel(override val name: String, private val bop: BindingOperation) : NamedModel {

  val style: WsdlStyle? = findStyle()
  val type: OperationType = findType()

  fun getFault(faultName: String): FaultModel {
    try {
      return FaultModel(bop.operation.getFault(faultName))
    } catch (e: Exception) {
      throw IllegalArgumentException("Could not retrieve fault [$name]", e)
    }
  }

  fun getInputMessage(): Message {
    return bop.operation.input.message
  }

  fun getOutputMessage(): Message {
    return bop.operation.output.message
  }

  fun getInputBodyPart(): Optional<Part> {
    return getBodyPart(getInputMessage(), bop.bindingInput)
  }

  fun getOutputBodyPart(): Optional<Part> {
    return getBodyPart(getOutputMessage(), bop.bindingOutput)
  }

  fun getInputHeaders(): List<SoapHeader> {
    return getHeaderParts(bop.bindingInput)
  }

  fun getOutputHeaders(): List<SoapHeader> {
    return getHeaderParts(bop.bindingOutput)
  }

  private fun findType(): OperationType {
    return when(bop.operation.style) {
      javax.wsdl.OperationType.NOTIFICATION -> OperationType.NOTIFICATION
      javax.wsdl.OperationType.SOLICIT_RESPONSE -> OperationType.SOLICIT_RESPONSE
      javax.wsdl.OperationType.ONE_WAY -> OperationType.ONE_WAY
      else -> OperationType.REQUEST_RESPONSE
    }
  }

  private fun findStyle(): WsdlStyle? {
    val style = bop.extensibilityElements
        .filter { e -> e is SOAP12Operation || e is SOAPOperation }
        .map { e -> if (e is SOAPOperation) e.style else (e as SOAP12Operation).style }
        .filter { it != null }
        .firstOrNull()
    return if (style != null) WsdlStyleFinder.find(style) else null
  }

  private fun getBodyPart(message: Message, bindingType: ElementExtensible): Optional<Part> {
    val parts = message.parts
    if (parts == null || parts.isEmpty()) {
      return Optional.empty()
    }
    if (parts.size == 1) {
      return Optional.ofNullable(parts[parts.keys.toTypedArray()[0]] as Part)
    }
    return getBodyPartName(bindingType).flatMap<Part> { partName -> Optional.ofNullable(parts[partName] as Part) }
  }

  private fun getBodyPartName(bindingType: ElementExtensible): Optional<String> {
    val elements = bindingType.extensibilityElements
    if (elements != null) {
      val bodyParts = elements
          .filter { e -> e is SOAPBody || e is SOAP12Body }
          .map { e -> if (e is SOAPBody) e.parts else (e as SOAP12Body).parts }
          .map { parts -> parts ?: emptyList<Part>() }
          .first()

      if (!bodyParts.isEmpty()) {
        return Optional.ofNullable(bodyParts[0] as String)
      }
    }
    return Optional.empty()
  }

  private fun getHeaderParts(bindingType: ElementExtensible): List<SoapHeader> {
    val extensible = bindingType.extensibilityElements
    if (extensible != null) {
      return extensible
          .filter { e -> e is SOAPHeader || e is SOAP12Header }
          .map { e -> if (e is SOAPHeader) SoapHeader(e) else SoapHeader(e as SOAP12Header) }
    }
    return emptyList()
  }
}
