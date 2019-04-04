package org.mule.wsdl.parser.operation

import org.mule.metadata.api.TypeLoader
import org.mule.wsdl.parser.model.FaultModel
import org.mule.wsdl.parser.model.WsdlStyle
import org.mule.wsdl.parser.model.message.MessageDefinition
import org.mule.wsdl.parser.model.operation.OperationModel
import org.mule.wsdl.parser.model.operation.OperationType
import org.mule.wsdl.parser.model.operation.OperationType.ONE_WAY
import org.mule.wsdl.parser.model.operation.Type
import org.mule.wsdl.parser.model.operation.Type.Companion.NULL_OPERATION_TYPE
import org.mule.wsdl.parser.model.version.SoapVersion
import org.mule.wsdl.parser.model.version.SoapVersion.*
import org.mule.wsdl.parser.operation.WsdlOperationTypeParser.Companion.parseInput
import org.mule.wsdl.parser.operation.WsdlOperationTypeParser.Companion.parseOutput
import javax.wsdl.BindingOperation
import javax.wsdl.Definition
import javax.wsdl.Fault
import javax.wsdl.extensions.soap.SOAPOperation
import javax.wsdl.extensions.soap12.SOAP12Operation

abstract class BaseWsdlOperationParser internal constructor(protected val wsdlStyle: WsdlStyle,
                                                            protected val bop: BindingOperation) {

  protected fun parseOperation(): OperationModel {

    val type = findType()

    if (wsdlStyle == WsdlStyle.RPC) {
      return OperationModel(bop.name, NULL_OPERATION_TYPE, NULL_OPERATION_TYPE, type, findAction(), findFaults(), findVersion())
    }

    return OperationModel(bop.name, getInputType(), getOutputType(), type, findAction(), findFaults(), findVersion())
  }

  abstract fun getInputType(): Type

  abstract fun getOutputType(): Type

  fun findFaults(): List<FaultModel> = bop.operation.faults
    .map { (_, f) -> f as Fault }.map { f -> FaultModel(f.name, MessageDefinition.fromMessage(f.message, null)) }

  internal fun findType(): OperationType {
    return when (bop.operation.style) {
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

  private fun findVersion(): SoapVersion? {
    return bop.extensibilityElements
      .filter { e -> e is SOAP12Operation || e is SOAPOperation }
      .map { e -> if (e is SOAPOperation) SOAP11 else SOAP12 }
      .filter { it != null }
      .firstOrNull()
  }
}

class WsdlTypelessOperationParser private constructor(style: WsdlStyle, bop: BindingOperation) : BaseWsdlOperationParser(style, bop) {

  override fun getInputType(): Type = Type.NULL_OPERATION_TYPE

  override fun getOutputType(): Type = Type.NULL_OPERATION_TYPE

  companion object {
    fun parse(wsdlStyle: WsdlStyle, bop: BindingOperation) = WsdlTypelessOperationParser(wsdlStyle, bop).parseOperation()
  }
}

class DefaultWsdlOperationParser private constructor(private val wsdl: Definition,
                                                     private val loader: TypeLoader,
                                                     wsdlStyle: WsdlStyle,
                                                     bop: BindingOperation): BaseWsdlOperationParser(wsdlStyle, bop)  {

  override fun getInputType(): Type = parseInput(wsdl, loader, bop)

  override fun getOutputType(): Type = if (findType() != ONE_WAY) parseOutput(wsdl, loader, bop) else NULL_OPERATION_TYPE

  companion object {
    fun parse(wsdl: Definition, wsdlStyle: WsdlStyle, loader: TypeLoader, bop: BindingOperation) = DefaultWsdlOperationParser(wsdl, loader, wsdlStyle, bop).parseOperation()
  }
}
