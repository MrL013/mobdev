package data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WsClient(private val sessionManager: SessionManager) {
    private val json = Json { ignoreUnknownKeys = true }

    fun connect(): Flow<MessageDto> = callbackFlow {
        val name = sessionManager.login ?: run {
            close()
            return@callbackFlow
        }
        val token = sessionManager.token ?: run {
            close()
            return@callbackFlow
        }

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("wss://faerytea.name/ws/$name?token=$token")
            .build()

        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val parsed = runCatching { json.decodeFromString<WsEventInNewMessage>(text) }.getOrNull()
                val msg = parsed?.NewMessage?.msg
                if (msg != null) trySend(msg)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = Unit
        })

        awaitClose {
            socket.close(1000, null)
            client.dispatcher.executorService.shutdown()
        }
    }
}


