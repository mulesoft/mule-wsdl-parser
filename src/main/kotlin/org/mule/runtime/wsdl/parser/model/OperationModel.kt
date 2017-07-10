package org.mule.runtime.wsdl.parser.model

import org.mule.runtime.wsdl.parser.model.MessagePartModel.PartType.*

class OperationModel(override val name: String,
                     val inputParts: List<MessagePartModel>,
                     val outputParts: List<MessagePartModel>): NamedModel {

  val inputBody = inputParts.find { p -> p.partType == BODY }
  val inputHeaders = inputParts.filter { p -> p.partType == HEADER }

  val outputBody = outputParts.find { p -> p.partType == BODY }
  val outputHeaders = outputParts.filter { p -> p.partType == HEADER }

  fun getInputPart(name: String) = inputParts.find { p -> p.partName == name }
  fun getOutputPart(name: String) = outputParts.find { p -> p.partName == name }
}
