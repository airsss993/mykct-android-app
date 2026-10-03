package ru.dzhaparidze.mykct.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import ru.dzhaparidze.mykct.BuildConfig
import ru.dzhaparidze.mykct.data.net.Http

/**
 * Короткая фраза под процентом посещаемости: её пишет модель в OpenRouter по самому
 * проценту, без имени, группы и прочего - наружу уходят только три числа.
 *
 * Клиент свой, а не общий `Http.client`: в debug-сборке общий движок подменён
 * заглушкой, и запрос к OpenRouter упирался бы в неё.
 *
 * Ключ лежит в `BuildConfig` и вместе с ним попадает в APK - вытащить его оттуда
 * может любой. Пока это личный ключ с лимитом на OpenRouter; правильное место для
 * такого запроса - свой бэкенд, который держит ключ у себя.
 */
object Motivation {

    /** Самая дешёвая из линейки DeepSeek на OpenRouter: 0.089 / 0.177 доллара за миллион. */
    private const val MODEL = "deepseek/deepseek-v4-flash"
    private const val URL = "https://openrouter.ai/api/v1/chat/completions"

    private const val PROMPT =
        "Ты пишешь одну строку для студенческого приложения колледжа. " +
            "Дай короткую фразу про посещаемость: от двух до пяти слов, по-русски, " +
            "обращение на ты, без эмодзи, без кавычек и без точки в конце. " +
            "Высокий процент хвали, низкий подбадривай без нравоучений."

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) { json(Http.json) }
            install(HttpTimeout) { requestTimeoutMillis = 8_000; connectTimeoutMillis = 5_000 }
        }
    }

    // Один процент - одна фраза за запуск: перелистывание месяцев не должно
    // дёргать модель по кругу и платить за одно и то же.
    private val cache = mutableMapOf<Int, String>()

    /** Фраза по статистике месяца. Без ключа, без сети и при любой ошибке - заготовка. */
    suspend fun line(stats: AttendanceStats): String {
        val percent = stats.percent
        cache[percent]?.let { return it }
        val fallback = fallback(percent)
        if (BuildConfig.OPENROUTER_KEY.isBlank()) return fallback

        val generated = runCatching { ask(percent, stats.present, stats.total) }.getOrNull()
        val line = generated?.takeIf { it.isNotBlank() } ?: fallback
        cache[percent] = line
        return line
    }

    private suspend fun ask(percent: Int, present: Int, total: Int): String? {
        val response = client.post(URL) {
            header("Authorization", "Bearer ${BuildConfig.OPENROUTER_KEY}")
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    model = MODEL,
                    messages = listOf(
                        Message("system", PROMPT),
                        Message("user", "Посещаемость $percent процентов: был на $present парах из $total."),
                    ),
                ),
            )
        }
        if (!response.status.isSuccess()) return null
        val body = Http.json.decodeFromString<ChatResponse>(response.bodyAsText())
        return body.choices.firstOrNull()?.message?.content?.trim()?.trim('"', '.')?.takeIf { it.length <= 60 }
    }

    /** Заготовки на случай, когда модель недоступна: фраза в карточке есть всегда. */
    private fun fallback(percent: Int): String = when {
        percent >= 95 -> "Ни одного пропуска"
        percent >= 85 -> "Почти идеальный месяц"
        percent >= 70 -> "Хорошо идёшь, не сбавляй"
        percent >= 50 -> "Половина есть, подтянись"
        else -> "Пора возвращаться на пары"
    }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val max_tokens: Int = 40,
        val temperature: Double = 0.9,
    )

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: Message? = null)
}
