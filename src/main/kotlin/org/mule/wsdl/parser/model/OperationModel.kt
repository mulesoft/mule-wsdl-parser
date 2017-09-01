package org.mule.wsdl.parser.model

import org.mule.metadata.api.builder.BaseTypeBuilder
import org.mule.metadata.api.model.BinaryType
import org.mule.metadata.api.model.MetadataFormat.XML
import org.mule.metadata.api.model.MetadataType
import org.mule.metadata.api.model.ObjectFieldType
import org.mule.metadata.api.model.ObjectType
import org.mule.wsdl.parser.model.MessagePartModel.PartType.*

class OperationModel(override val name: String,
                     inputParts: List<MessagePartModel>,
                     outputParts: List<MessagePartModel>) : NamedModel {

  private companion object {
    val TYPE_BUILDER: BaseTypeBuilder = BaseTypeBuilder.create(XML)
    val NULL_TYPE = TYPE_BUILDER.nullType().build()!!
  }

  private val inputBodyPart = inputParts.find { p -> p.partType == BODY }
  private val outputBodyPart = outputParts.find { p -> p.partType == BODY }

  val bodyInputType: MetadataType = calculateBody(inputBodyPart)
  val bodyOutputType: MetadataType = calculateBody(outputBodyPart)

  val headersInputType: MetadataType = calculateHeaders(inputParts)
  val headersOutputType: MetadataType = calculateHeaders(outputParts)

  val attachmentsInputType: MetadataType = calculateAttachments(inputBodyPart)
  val attachmentsOutputType: MetadataType = calculateAttachments(outputBodyPart)

  private fun calculateHeaders(parts: List<MessagePartModel>): MetadataType {
    val headers = parts.filter { p -> p.partType == HEADER }
    if (headers.isEmpty()) return NULL_TYPE

    val type = TYPE_BUILDER.objectType()
    headers.forEach({ header -> type.addField().key(header.partName).value(header.type) })
    return type.build()
  }

  private fun calculateAttachments(body: MessagePartModel?): MetadataType {
    if (body == null) return NULL_TYPE

    val operationType = getOperationEnclosedType(body.type)
    val attachments = getAttachmentFields(operationType)

    if (attachments.isEmpty()) return NULL_TYPE

    val type = TYPE_BUILDER.objectType()
    attachments.forEach { attachment -> type.addField().key(attachment.key.name.localPart).value(attachment.value) }
    return type.build()
  }

  private fun calculateBody(body: MessagePartModel?): MetadataType {
    if (body == null) return NULL_TYPE
    val operationType = getOperationEnclosedType(body.type)
    val attachments = getAttachmentFields(operationType)
    attachments.forEach { a -> operationType.fields.removeIf({ f -> f.key.name?.localPart == a.key.name?.localPart }) }
    return if (operationType.fields.isEmpty()) NULL_TYPE else operationType
  }

  // REVIEW
  fun getOperationEnclosedType(body: MetadataType): ObjectType {
    when (body) {
      is ObjectType -> if (body.fields.size == 1) return body.fields.iterator().next().value as ObjectType
    }
    throw IllegalArgumentException("Could not find soap operation element in the provided body MetadataType")
  }

  // REVIEW
  fun getAttachmentFields(operationType: ObjectType): List<ObjectFieldType> {
    return operationType.fields.filter { field -> field.value is BinaryType }
  }
}
