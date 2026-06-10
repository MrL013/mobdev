package data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class LoginRequest(val name: String, val pwd: String)

@Serializable
data class MessageDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String? = null,
    val from: String,
    val to: String? = "1@channel",
    val data: MessageData,
    @Serializable(with = FlexibleStringSerializer::class)
    val time: String? = null
)

@Serializable
data class MessageData(
    @SerialName("Text") val text: TextData? = null,
    @SerialName("Image") val image: ImageData? = null
)

@Serializable
data class TextData(val text: String)

@Serializable
data class ImageData(val link: String? = null)

object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
        val primitive = jsonDecoder?.decodeJsonElement() as? JsonPrimitive
        return primitive?.content ?: decoder.decodeString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

