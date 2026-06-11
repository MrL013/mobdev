package data

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @POST("login")
    suspend fun login(@Body body: LoginRequest): Response<ResponseBody>

    @POST("logout")
    suspend fun logout(): Response<Unit>

    @GET("channels")
    suspend fun channels(): Response<List<String>>

    @GET("channel/{channel}")
    suspend fun messages(
        @Path("channel") channel: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String? = null,
        @Query("reverse") reverse: Boolean = false
    ): Response<List<MessageDto>>

    @POST("messages")
    suspend fun postMessage(@Body message: MessageDto): Response<ResponseBody>

    @POST("messages")
    suspend fun postMultipartMessage(@Body body: MultipartBody): Response<ResponseBody>
}
