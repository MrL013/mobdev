package data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.HttpException

class UnauthorizedException : Exception()
class OfflineException : Exception("No network connection")
class ApiException(code: Int, body: String?) : Exception(
    buildString {
        append("HTTP ")
        append(code)
        if (!body.isNullOrBlank()) {
            append(": ")
            append(body)
        }
    }
)

class ChatRepository(
    private val api: ChatApi,
    private val wsClient: WsClient,
    private val sessionManager: SessionManager,
    private val cache: ChatCache,
    private val networkMonitor: NetworkMonitor
) {
    companion object {
        const val MESSAGES_PAGE_SIZE = 20
    }

    val currentLogin: String
        get() = sessionManager.login.orEmpty()

    suspend fun login(name: String, pwd: String): Result<Unit> = runCatching {
        val response = api.login(LoginRequest(name, pwd))
        if (response.code() == 401) {
            sessionManager.clearAll()
            throw UnauthorizedException()
        }
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
        if (!networkMonitor.isOnline() && !sessionManager.token.isNullOrBlank()) {
            return Result.success(Unit)
        }
        return login(name, pwd)
    }

    suspend fun channels(): Result<List<String>> {
        if (!networkMonitor.isOnline()) return Result.success(cache.loadChannels())
        val result = authCall { api.channels() }
        result.onSuccess { cache.saveChannels(it) }
        return result.fallbackToCache { cache.loadChannels() }
    }

    suspend fun messages(channel: String, lastKnownId: String?): Result<List<MessageDto>> {
        val cached = cache.loadMessagePage(channel, lastKnownId, MESSAGES_PAGE_SIZE)
        if (!networkMonitor.isOnline()) return Result.success(cached)
        val result = authCall {
            api.messages(
                channel = channel,
                limit = MESSAGES_PAGE_SIZE,
                lastKnownId = lastKnownId,
                reverse = true
            )
        }
        result.onSuccess { cache.mergeAndSaveMessages(channel, it) }
        return result.fallbackToCache { cached }
    }

    suspend fun sendMessage(chat: String, text: String): Result<Unit> = runCatching {
        if (!networkMonitor.isOnline()) throw OfflineException()
        val response = api.postMessage(
            MessageDto(
                from = currentLogin,
                to = chat,
                data = MessageData(text = TextData(text))
            )
        )
        if (response.code() == 401) {
            sessionManager.clearAuth()
            throw UnauthorizedException()
        }
        if (!response.isSuccessful) throw ApiException(response.code(), response.errorBody()?.string())
    }

    suspend fun sendImageMessage(chat: String, imageUri: Uri, contentResolver: ContentResolver): Result<Unit> = runCatching {
        if (!networkMonitor.isOnline()) throw OfflineException()
        val name = sessionManager.login.orEmpty()
        val raw = contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: error("Cannot read image")
        val mimeType = contentResolver.getType(imageUri) ?: "image/*"
        val fileName = imageFileName(mimeType)
        val message = MessageDto(
            from = name,
            to = chat,
            data = MessageData(image = ImageData(link = fileName))
        )
        val msgJson = Json.encodeToString(message)
        val picBody = raw.toRequestBody(mimeType.toMediaType())

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "msg",
                null,
                msgJson.toRequestBody("application/json".toMediaType())
            )
            .addFormDataPart("picture", fileName, picBody)
            .build()
        val response = api.postMultipartMessage(body)
        if (response.code() == 401) {
            sessionManager.clearAuth()
            throw UnauthorizedException()
        }
        if (!response.isSuccessful) throw ApiException(response.code(), response.errorBody()?.string())
    }

    fun observeIncomingMessages() = wsClient.connect()

    fun observeNetwork() = networkMonitor.observeOnline()

    fun cacheMessages(channel: String, messages: List<MessageDto>) {
        cache.saveMessages(channel, messages)
    }

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
        if (!response.isSuccessful) throw ApiException(response.code(), response.errorBody()?.string())
        response.body() ?: throw IllegalStateException("Empty body")
    }

    private fun <T> Result<T>.fallbackToCache(loader: () -> T): Result<T> {
        val error = exceptionOrNull()
        if (isSuccess || error is UnauthorizedException) return this
        val cached = loader()
        return if (cached is Collection<*> && cached.isEmpty()) this else Result.success(cached)
    }

    private fun imageFileName(mimeType: String): String {
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        return "image_${System.currentTimeMillis()}.$extension"
    }
}
