package ru.dzhaparidze.mykct.data.net

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import ru.dzhaparidze.mykct.BuildConfig
import java.io.IOException

/**
 * Ошибка запроса с человеческим текстом — его показывает UI.
 * [status] null, если до сервера вообще не дошли.
 */
class ApiException(val status: Int?, message: String, val code: String? = null) : Exception(message)

/** Корень всех маршрутов mykct-api: расписание, auth, посещаемость, пуши. */
val API_URL = "${BuildConfig.API_BASE_URL}/api/mykct/v1"

/**
 * Один HTTP-клиент на приложение. `expectSuccess = false` намеренно: коды разбираем
 * сами в [decode], потому что бэкенд кладёт ошибку в конверт `{"code", "message", "details"}`.
 *
 * Движок берётся из `httpEngine()` — он свой у debug и release: в debug это заглушка
 * вместо сервера, пока боевого адреса нет.
 */
object Http {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val client: HttpClient by lazy {
        HttpClient(httpEngine()) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
        }
    }
}

/** Разбор ответа: успех — тело, иначе [ApiException] с текстом и кодом из конверта ошибки. */
suspend inline fun <reified T> HttpResponse.decode(): T {
    ensureSuccess()
    return try {
        body()
    } catch (e: SerializationException) {
        throw ApiException(status.value, "Не удалось разобрать ответ сервера")
    }
}

/** Ответ без тела (204, signout): проверяем только код. */
suspend fun HttpResponse.ensureSuccess() {
    if (status.isSuccess()) return
    val envelope = runCatching { Http.json.parseToJsonElement(bodyAsText()).jsonObject }.getOrNull()
    throw ApiException(
        status = status.value,
        message = envelope?.get("message")?.jsonPrimitive?.content ?: errorText(status.value),
        code = envelope?.get("code")?.jsonPrimitive?.content,
    )
}

/** Текст ошибки по коду ответа - когда сервер своего не прислал (nginx, обрыв). */
fun errorText(code: Int): String {
    return when (code) {
        401 -> "Требуется авторизация"
        403 -> "Доступ запрещён"
        404 -> "Ресурс не найден"
        429 -> "Слишком много запросов, попробуйте через минуту"
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
