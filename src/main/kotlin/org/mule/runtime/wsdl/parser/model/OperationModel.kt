package org.mule.runtime.wsdl.parser.model

class OperationModel(override val name: String, val inputType: TypeModel, val outputType: TypeModel): NamedModel
