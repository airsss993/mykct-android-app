package ru.dzhaparidze.mykct.data.auth

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.dzhaparidze.mykct.BuildConfig
import io.ktor.http.isSuccess
import ru.dzhaparidze.mykct.data.net.ApiException
import ru.dzhaparidze.mykct.data.net.Http
import ru.dzhaparidze.mykct.data.net.apiCall
import ru.dzhaparidze.mykct.data.net.decode
import ru.dzhaparidze.mykct.data.net.errorText

/**
 * Auth-сервис колледжа. Пути и тела — как в iOS-версии
 * (`Features/Auth/Services/AuthAPI.swift`), у неё контракт уже боевой.
 *
 * Логины, пароли и токены не логируются нигде — ни в исключениях, ни в отладке.
 */
class AuthApi(private val baseUrl: String = BuildConfig.AUTH_BASE_URL) {

    @Serializable
    private data class Credentials(val username: String, val password: String)

    @Serializable
    private data class RefreshBody(@SerialName("refresh_token") val refreshToken: String)

    suspend fun signIn(username: String, password: String): SignInResponse = apiCall {
        Http.client.post("$baseUrl/auth/api/v1/app/signin") {
            contentType(ContentType.Application.Json)
            setBody(Credentials(username, password))
        }.decode()
    }

    suspend fun accessToken(refreshToken: String): AccessTokenResponse = apiCall {
        Http.client.post("$baseUrl/auth/api/v1/app/access") {
            contentType(ContentType.Application.Json)
            setBody(RefreshBody(refreshToken))
        }.decode()
    }

    suspend fun refreshRefreshToken(refreshToken: String): RefreshTokenResponse = apiCall {
        Http.client.post("$baseUrl/auth/api/v1/app/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshBody(refreshToken))
        }.decode()
    }

    /** Тело ответа пустое — интересен только код. */
    suspend fun signOut(refreshToken: String): Unit = apiCall {
        val response = Http.client.post("$baseUrl/auth/api/v1/app/signout") {
            contentType(ContentType.Application.Json)
            setBody(RefreshBody(refreshToken))
        }
        if (!response.status.isSuccess()) {
            throw ApiException(response.status.value, response.errorText(response.status.value))
        }
    }
}
