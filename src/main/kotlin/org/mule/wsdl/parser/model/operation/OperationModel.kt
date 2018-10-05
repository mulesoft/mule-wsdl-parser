package org.mule.wsdl.parser.model.operation

import org.mule.wsdl.parser.model.FaultModel
import org.mule.wsdl.parser.model.NamedModel
import java.util.*

class OperationModel(override val name: String, val inputType: Type,  val outputType: Type,
                     val type: OperationType, val soapAction: String?, val faults: List<FaultModel>) : NamedModel {

  fun getFault(faultName: String): Optional<FaultModel> = Optional.ofNullable(faults.find { f -> faultName == f.name })

}
