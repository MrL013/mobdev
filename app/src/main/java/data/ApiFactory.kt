package data

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

class ApiFactory(private val sessionManager: SessionManager) {
    fun createApi(): ChatApi {
        val tokenInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            sessionManager.token?.let { requestBuilder.addHeader("X-Auth-Token", it) }
            chain.proceed(requestBuilder.build())
        }

        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder().addInterceptor(tokenInterceptor).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://faerytea.name/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ChatApi::class.java)
    }
}


