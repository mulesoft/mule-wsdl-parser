package org.mule.wsdl.parser

import org.mule.metadata.api.TypeLoader
import org.mule.wsdl.parser.WsdlOperationTypeParser.Companion.NULL_TYPE
import org.mule.wsdl.parser.WsdlOperationTypeParser.Companion.parseInput
import org.mule.wsdl.parser.WsdlOperationTypeParser.Companion.parseOutput
import org.mule.wsdl.parser.model.*
import org.mule.wsdl.parser.model.message.MessageDefinition
import org.mule.wsdl.parser.model.operation.OperationModel
import org.mule.wsdl.parser.model.operation.OperationType
import org.mule.wsdl.parser.model.operation.OperationType.ONE_WAY
import org.mule.wsdl.parser.model.operation.Type
import org.mule.wsdl.parser.model.operation.Type.Companion.NULL_OPERATION_TYPE
import javax.wsdl.*
import javax.wsdl.extensions.soap.*
import javax.wsdl.extensions.soap12.*


class WsdlOperationParser private constructor(private val wsdl: Definition,
                                              private val wsdlStyle: WsdlStyle,
                                              private val loader: TypeLoader,
                                              private val bop: BindingOperation) {

  private fun parseOperation(): OperationModel {

    val type = findType()

    if (wsdlStyle == WsdlStyle.RPC) {
      return OperationModel(bop.name, NULL_OPERATION_TYPE, NULL_OPERATION_TYPE, type, findAction(), findFaults())
    }

    val inputType = parseInput(wsdl, loader, bop)
    val outputType = if (type != ONE_WAY) parseOutput(wsdl, loader, bop) else NULL_OPERATION_TYPE
    return OperationModel(bop.name, inputType, outputType, type, findAction(), findFaults())
  }

  fun findFaults(): List<FaultModel> = bop.operation.faults
          .map { (_, f) -> f as Fault }.map { f -> FaultModel(f.name, MessageDefinition.fromMessage(f.message)) }

  private fun findType(): OperationType {
    return when(bop.operation.style) {
      javax.wsdl.OperationType.NOTIFICATION -> OperationType.NOTIFICATION
      javax.wsdl.OperationType.SOLICIT_RESPONSE -> OperationType.SOLICIT_RESPONSE
      javax.wsdl.OperationType.ONE_WAY -> ONE_WAY
      else -> OperationType.REQUEST_RESPONSE
    }
  }

  private fun findAction(): String? {
    return bop.extensibilityElements
            .filter { e -> e is SOAP12Operation || e is SOAPOperation }
            .map { e -> if (e is SOAPOperation) e.soapActionURI else (e as SOAP12Operation).soapActionURI }
            .filter { it != null }
            .firstOrNull()
  }

  companion object {
    fun parse(wsdl: Definition, wsdlStyle: WsdlStyle, loader: TypeLoader, bop: BindingOperation) = WsdlOperationParser(wsdl, wsdlStyle, loader, bop).parseOperation()
  }
}
