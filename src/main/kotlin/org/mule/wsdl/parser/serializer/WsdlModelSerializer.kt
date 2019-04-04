package org.mule.wsdl.parser.serializer

import com.google.gson.GsonBuilder
import org.mule.metadata.api.model.MetadataType
import org.mule.metadata.persistence.MetadataTypeGsonTypeAdapter
import org.mule.wsdl.parser.model.WsdlModel

object WsdlModelSerializer {

  fun serialize(model: WsdlModel, pretty: Boolean = false): String {
    val gson = GsonBuilder()
    gson.registerTypeAdapter(MetadataType::class.java, MetadataTypeGsonTypeAdapter())
    if (pretty) {
      gson.setPrettyPrinting()
    }
    return gson.create().toJson(model)
  }

  fun deserialize(model: String): WsdlModel {
    return GsonBuilder()
      .registerTypeAdapter(MetadataType::class.java, MetadataTypeGsonTypeAdapter())
      .create()
      .fromJson(model, WsdlModel::class.java)
  }
}
