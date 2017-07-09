package org.mule.runtime.wsdl.parser

import javax.wsdl.BindingOperation
import javax.wsdl.Definition
import javax.wsdl.Part
import javax.wsdl.extensions.soap.SOAPBody
import javax.wsdl.extensions.soap12.SOAP12Body

internal class OperationTypeParser(private val definition: Definition) {

  fun getType(bop: BindingOperation) = ""

  private fun getBodyPart(bop: BindingOperation): Part? {
    // Pattern Matching
    val message = bop.operation.input.message
    val parts = message.parts as Map<String, Part>?
    if (parts == null || parts.isEmpty()) {
      return null
    }
    if (parts.size == 1) {
      return parts[parts.keys.toTypedArray()[0]]
    }
    return parts[getBodyPartName(bop)]
  }

  private fun getBodyPartName(bop: BindingOperation): String? {
    // Pattern matching
    val elements = bop.bindingInput.extensibilityElements
    if (elements != null) {
      val bodyParts = elements.stream()
          .filter { e -> e is SOAPBody || e is SOAP12Body }
          .map { e -> if (e is SOAPBody) e.parts else (e as SOAP12Body).parts }
          .map { parts -> parts ?: emptyList<Any>() }
          .findFirst()

      if (bodyParts.isPresent && !bodyParts.get().isEmpty()) {
        return bodyParts.get()[0] as String
      }
    }
    return null
  }
}
