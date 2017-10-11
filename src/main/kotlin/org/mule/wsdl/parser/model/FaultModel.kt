package org.mule.wsdl.parser.model

import javax.wsdl.Fault

class FaultModel(private val fault: Fault) {

  fun getName() = fault.name

  fun getMessage() = fault.message
}
