package ru.dzhaparidze.mykct.data

import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import ru.dzhaparidze.mykct.BuildConfig
import ru.dzhaparidze.mykct.data.net.Http
import ru.dzhaparidze.mykct.data.net.apiCall
import ru.dzhaparidze.mykct.data.net.decode
import java.time.LocalDate
import java.time.LocalTime

/**
 * Расписание из GET /api/v1/schedule. Токен не нужен — эндпоинт публичный.
 *
 * Ключи с капитализацией портала (`ClID`, `Day`, `SubGroup`) остаются здесь и наружу
 * не выходят: экран работает с [Lesson].
 */
class ApiScheduleRepository(private val baseUrl: String = BuildConfig.API_BASE_URL) : ScheduleRepository {

    override suspend fun weekSchedule(monday: LocalDate, selection: Selection): List<Lesson> = apiCall {
        val response: ScheduleResponse = Http.client.get("$baseUrl/api/v1/schedule") {
            parameter("group", selection.group)
            parameter("start", monday.toString())
            parameter("end", monday.plusDays(6).toString())
            // «не слать вовсе» вместо «*»: бэк понимает оба варианта, но так короче URL
            selection.subgroup?.let { parameter("subgroup", it) }
            selection.englishGroup?.let { parameter("english_group", it) }
            selection.profileSubgroup?.let { parameter("profile_subgroup", it) }
        }.decode()
        response.events.mapNotNull { it.toLesson() }
    }

    override suspend fun classDetails(id: String): List<Pair<String, String>> = apiCall {
        val details: JsonObject = Http.client.get("$baseUrl/api/v1/classdetails") {
            parameter("id", id)
        }.decode()
        details.flattenDetails()
    }
}

/**
 * Детали пары портал отдаёт произвольным объектом: бэкенд его не типизирует
 * (`FetchClassDetails` возвращает `map[string]any` и льёт как есть), в iOS эндпоинт
 * не используется, живого портала под рукой нет. Поэтому не выдуманная модель, а
 * плоский список «ключ → значение»: листья берутся как есть, пустые значения и то,
 * что уже нарисовано на карточке, выбрасываются.
 *
 * ponytail: подписи — сырые ключи портала. Появится настоящий ответ — сюда придёт
 * словарь русских названий, разбор менять не придётся.
 */
internal fun JsonObject.flattenDetails(): List<Pair<String, String>> = buildList {
    fun walk(element: JsonElement, key: String) {
        when (element) {
            is JsonObject -> element.forEach { (name, value) -> walk(value, name) }
            is JsonArray -> element.forEach { walk(it, key) }
            is JsonPrimitive -> {
                val text = element.content.trim()
                if (key !in SHOWN_KEYS && text.isNotBlank() && text != "null" && text != "—") {
                    add(key to text)
                }
            }
        }
    }
    walk(this@flattenDetails, "")
}

/** Ключи, которые уже видно в карточке пары: дублировать их в деталях незачем. */
private val SHOWN_KEYS = setOf(
    "ClID", "SClID", "Day", "start", "end", "title", "topic", "room", "group", "color", "type",
)

@Serializable
private data class ScheduleResponse(val events: List<EventDto> = emptyList())

@Serializable
private data class EventDto(
    @SerialName("ClID") val id: String = "",
    @SerialName("Day") val day: String = "",
    val start: String = "",
    val end: String = "",
    val title: String = "",
    val topic: String = "",
    val room: String = "",
    val color: String? = null,
    val type: String? = null,
    @SerialName("SubGroup") val subGroups: List<SubGroupDto>? = null,
)

@Serializable
private data class SubGroupDto(
    @SerialName("SClID") val id: String = "",
    @SerialName("SGrID") val groupId: String = "",
    @SerialName("SGCaID") val room: String = "",
    @SerialName("STopic") val topic: String = "",
    @SerialName("STitle") val title: String = "",
)

/** Пара с непарсящейся датой или временем выбрасывается: рисовать её всё равно негде. */
private fun EventDto.toLesson(): Lesson? {
    val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return null
    val from = parseTime(start) ?: return null
    val to = parseTime(end) ?: return null
    return Lesson(
        id = id.ifBlank { "$day-$start-$title" },
        date = date,
        start = from,
        end = to,
        title = title,
        topic = topic,
        room = room.takeIf { it != "—" }.orEmpty(),
        colorHex = color?.takeIf { it.startsWith("#") },
        subgroups = subGroups.orEmpty().map {
            LessonSubgroup(id = it.groupId, title = it.title, topic = it.topic, room = it.room)
        },
    )
}

/**
 * Время из портала. Штатный `LocalTime.parse` тут не годится: приходит и «09:00», и «9:00»,
 * а в /attendance — целиком «2026-09-01 09:00».
 */
internal fun parseTime(value: String): LocalTime? {
    val parts = value.trim().substringAfterLast(' ').split(":")
    if (parts.size < 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return runCatching { LocalTime.of(hour, minute) }.getOrNull()
}
