package doichin.auth.client

import doichin.auth.dto.LoginRequest
import doichin.auth.dto.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import kotlin.uuid.Uuid


val defaultHttpClient by lazy {
    HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                serializersModule = serializersModuleOf(Uuid.serializer())
            })
        }
    }
}

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T, val statusCode: Int) : ApiResponse<T>()
    data class Failure(val statusCode: Int, val errorBody: String?) : ApiResponse<Nothing>()
    data class Exception(val throwable: Throwable) : ApiResponse<Nothing>()
}

class AuthClient(
    val authUrl: Url,
    val client: HttpClient = defaultHttpClient
) {
    var authToken: String? = null
    val isLoggedIn: Boolean get() = authToken != null

    suspend inline fun <reified T> get(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): ApiResponse<T> {
        return try {
            val response = client.get(url) {
                headers {
                    contentType(ContentType.Application.Json)
                    if (isLoggedIn) {
                        append("Authorization", "Bearer $authToken")
                    }
                }

                block()
            }

            processResponse(response)
        } catch (e: Throwable) {
            ApiResponse.Exception(e)
        }
    }

    suspend inline fun <reified T> post(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): ApiResponse<T> {
        return try {
            val response = client.post(url) {
                headers {
                    contentType(ContentType.Application.Json)
                    if (isLoggedIn) {
                        append("Authorization", "Bearer $authToken")
                    }
                }
                block()
            }

            processResponse(response)
        } catch (e: Throwable) {
            ApiResponse.Exception(e)
        }
    }

    suspend inline fun<reified T> processResponse(response: HttpResponse): ApiResponse<T> {
        if (response.status.isSuccess()) {
            val data: T = response.body()
            return ApiResponse.Success(data, response.status.value)
        } else {
            val errorBody = response.body<String?>()
            return ApiResponse.Failure(response.status.value, errorBody)
        }
    }

    suspend fun login(username: String, password: String): ApiResponse<LoginResponse> {
        val resp = post<LoginResponse>("$authUrl/apps/auth/users/login") {
            setBody(LoginRequest(username, password))
        }

        when (resp) {
            is ApiResponse.Success -> {
                authToken = resp.data.token
            }
            else -> { }
        }

        return resp
    }

    fun logout() {
        authToken = null
    }
}