package data

import kotlinx.serialization.Serializable

@Serializable
data class WsEventOutStartTyping(val StartTyping: StartTypingPayload)

@Serializable
data class StartTypingPayload(val chat: String)

@Serializable
data class WsEventOutEndTyping(val EndTyping: EndTypingPayload = EndTypingPayload())

@Serializable
data class EndTypingPayload(val nothing: String = "")

@Serializable
data class WsEventInNewMessage(val NewMessage: NewMessagePayload? = null)

@Serializable
data class NewMessagePayload(val msg: MessageDto)


