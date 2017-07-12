package org.mule.runtime.wsdl.parser.model

import jdk.Exported
import org.mule.metadata.api.model.MetadataType

class MessagePartModel(val partName: String, val type: MetadataType, val partType: PartType) {

  enum class PartType {
    HEADER,
    BODY
  }
}
