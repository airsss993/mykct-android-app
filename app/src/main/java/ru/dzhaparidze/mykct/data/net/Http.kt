package ru.dzhaparidze.mykct.data.net

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

/**
 * Ошибка запроса с человеческим текстом — его показывает UI.
 * [status] null, если до сервера вообще не дошли.
 */
class ApiException(val status: Int?, message: String) : Exception(message)

/**
 * Один HTTP-клиент на приложение. `expectSuccess = false` намеренно: коды разбираем
 * сами в [decode], потому что бэкенд кладёт текст ошибки в тело `{"error": "..."}`.
 */
object Http {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
        }
    }
}

/** Разбор ответа: успех — тело, иначе [ApiException] с текстом из `{"error": ...}`. */
suspend inline fun <reified T> HttpResponse.decode(): T {
    if (!status.isSuccess()) throw ApiException(status.value, errorText(status.value))
    return try {
        body()
    } catch (e: SerializationException) {
        throw ApiException(status.value, "Не удалось разобрать ответ сервера")
    }
}

/** Текст ошибки от бэкенда, а если его нет — по коду ответа. */
suspend fun HttpResponse.errorText(code: Int): String {
    val fromBody = runCatching {
        Http.json.parseToJsonElement(bodyAsText()).jsonObject["error"]?.jsonPrimitive?.content
    }.getOrNull()
    return fromBody ?: when (code) {
        401 -> "Требуется авторизация"
        403 -> "Доступ запрещён"
        404 -> "Ресурс не найден"
        in 500..599 -> "Ошибка сервера ($code)"
        else -> "Сервер вернул код $code"
    }
}

/** Сетевые сбои наружу тоже уходят как [ApiException] — UI знает только его. */
suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (e: ApiException) {
    throw e
} catch (e: IOException) {
    throw ApiException(null, "Нет связи с сервером")
}
