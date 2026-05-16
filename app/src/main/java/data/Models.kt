package data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val name: String, val pwd: String)

@Serializable
data class MessageDto(
    val id: String? = null,
    val from: String,
    val to: String? = "1@channel",
    val data: MessageData,
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


