package data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class UnauthorizedException : Exception()

class ChatRepository(
    private val api: ChatApi,
    private val wsClient: WsClient,
    private val sessionManager: SessionManager
) {
    suspend fun login(name: String, pwd: String): Result<Unit> = runCatching {
        val response = api.login(LoginRequest(name, pwd))
        if (!response.isSuccessful) throw HttpException(response)
        val token = response.body()?.string()?.trim().orEmpty()
        if (token.isBlank()) throw IllegalStateException("Empty token")
        sessionManager.login = name
        sessionManager.password = pwd
        sessionManager.token = token
    }

    suspend fun loginWithSavedCredentials(): Result<Unit> {
        val name = sessionManager.login ?: return Result.failure(IllegalStateException("No saved login"))
        val pwd = sessionManager.password ?: return Result.failure(IllegalStateException("No saved password"))
        return login(name, pwd)
    }

    suspend fun channels(): Result<List<String>> = authCall { api.channels() }

    suspend fun messages(channel: String, lastKnownId: String?): Result<List<MessageDto>> = authCall {
        api.messages(channel = channel, lastKnownId = lastKnownId, reverse = true)
    }

    suspend fun sendMessage(chat: String, text: String): Result<Unit> = authCall {
        api.postMessage(
            MessageDto(
                from = sessionManager.login.orEmpty(),
                to = chat,
                data = MessageData(text = TextData(text))
            )
        )
    }.map { }

    suspend fun sendImageMessage(chat: String, imageUri: Uri, contentResolver: ContentResolver): Result<Unit> = runCatching {
        val name = sessionManager.login.orEmpty()
        val raw = contentResolver.openInputStream(imageUri)?.readBytes() ?: error("Cannot read image")
        val message = MessageDto(from = name, to = chat, data = MessageData(image = ImageData()))
        val msgJson = Json.encodeToString(message)
        val msgBody = msgJson.toRequestBody("application/json".toMediaType())
        val picBody = raw.toRequestBody("image/*".toMediaType())
        val part = MultipartBody.Part.createFormData("picture", "upload.jpg", picBody)

        val response = api.postImageMessage(msgBody, part)
        if (response.code() == 401) {
            sessionManager.clearAuth()
            throw UnauthorizedException()
        }
        if (!response.isSuccessful) throw HttpException(response)
    }

    fun observeIncomingMessages() = wsClient.connect()

    suspend fun logout(): Result<Unit> = runCatching {
        api.logout()
        sessionManager.clearAll()
    }

    private suspend fun <T> authCall(block: suspend () -> retrofit2.Response<T>): Result<T> = runCatching {
        val response = block()
        if (response.code() == 401) {
            sessionManager.clearAuth()
            throw UnauthorizedException()
        }
        if (!response.isSuccessful) throw HttpException(response)
        response.body() ?: throw IllegalStateException("Empty body")
    }
}