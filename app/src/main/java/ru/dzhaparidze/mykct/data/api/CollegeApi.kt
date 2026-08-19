package ru.dzhaparidze.mykct.data.api

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import ru.dzhaparidze.mykct.BuildConfig
import ru.dzhaparidze.mykct.data.auth.AuthService
import ru.dzhaparidze.mykct.data.net.Http
import ru.dzhaparidze.mykct.data.net.apiCall
import ru.dzhaparidze.mykct.data.net.decode
import ru.dzhaparidze.mykct.data.parseTime
import java.time.LocalDate

/**
 * Эндпоинты, которым нужен токен: посещаемость, стрик, успеваемость.
 * Токен подставляет [AuthService.withToken] — он же повторит запрос после 401.
 */
class CollegeApi(
    private val auth: AuthService,
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) {

    suspend fun attendance(start: LocalDate, end: LocalDate): List<AttendanceRecord> = apiCall {
        auth.withToken { token ->
            // список может прийти голым `null`, а не `[]` — см. AttendanceService в college-app-core
            val dto: List<AttendanceDto>? = Http.client.get("$baseUrl/api/v1/attendance") {
                bearer(token)
                parameter("start", start.toString())
                parameter("end", end.toString())
            }.decode()
            dto.orEmpty().mapNotNull { it.toRecord() }.sortedWith(compareBy({ it.date }, { it.start }))
        }
    }

    suspend fun streak(): Streak = apiCall {
        auth.withToken { token ->
            Http.client.get("$baseUrl/api/v1/attendance/streak") { bearer(token) }
                .decode<StreakDto>()
                .toStreak()
        }
    }

    suspend fun subjects(): List<Subject> = apiCall {
        auth.withToken { token ->
            Http.client.get("$baseUrl/api/v1/performance/subjects") { bearer(token) }
                .decode<List<SubjectDto>?>()
                .orEmpty()
                .map { Subject(id = it.suId, title = it.title) }
        }
    }

    suspend fun scores(subjectId: String, start: LocalDate, end: LocalDate): List<SubjectLesson> = apiCall {
        auth.withToken { token ->
            val body: JsonElement = Http.client.post("$baseUrl/api/v1/performance/score") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(ScoreRequest(subjectId, start.toString(), end.toString()))
            }.decode()
            parseScores(body)
        }
    }
}

private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
    header("Authorization", "Bearer $token")
}

@Serializable
private data class ScoreRequest(
    @SerialName("SuID") val suId: String,
    val datastart: String,
    val dataend: String,
)

@Serializable
private data class AttendanceDto(
    @SerialName("ClID") val id: Long = 0,
    @SerialName("Day") val day: String = "",
    val start: String = "",
    val end: String = "",
    val title: String = "",
    val topic: String? = null,
    val room: String = "",
    val status: Int = -1,
)

private fun AttendanceDto.toRecord(): AttendanceRecord? {
    val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return null
    return AttendanceRecord(
        id = id.toString(),
        date = date,
        start = parseTime(start),
        end = parseTime(end),
        title = title,
        topic = topic.orEmpty(),
        room = room.takeIf { it != "—" }.orEmpty(),
        attendance = Attendance.of(status),
    )
}

@Serializable
private data class StreakDto(
    @SerialName("current_streak") val current: Int = 0,
    @SerialName("longest_streak") val longest: Int = 0,
    @SerialName("total_days_attended") val daysAttended: Int = 0,
    @SerialName("total_school_days") val schoolDays: Int = 0,
    @SerialName("last_attended_date") val lastAttended: String? = null,
    @SerialName("period_start") val periodStart: String? = null,
    @SerialName("period_end") val periodEnd: String? = null,
)

private fun StreakDto.toStreak() = Streak(
    current = current,
    longest = longest,
    daysAttended = daysAttended,
    schoolDays = schoolDays,
    // attendance_rate бэкенд отдаёт долей (0.5 = 50%), а не процентами — считаем сами
    rate = if (schoolDays > 0) daysAttended * 100.0 / schoolDays else 0.0,
    lastAttended = date(lastAttended),
    periodStart = date(periodStart),
    periodEnd = date(periodEnd),
)

@Serializable
private data class SubjectDto(
    @SerialName("SuID") val suId: String = "",
    @SerialName("SuIDcrc") val suIdCrc: String = "",
    @SerialName("Title") val title: String = "",
)

@Serializable
private data class ScoreDto(
    @SerialName("DateF") val dateF: String? = null,
    @SerialName("DateP") val dateP: String? = null,
    @SerialName("Score") val score: String = "",
    @SerialName("MaxScore") val max: Int = 0,
    @SerialName("Description") val description: String = "",
)

private fun ScoreDto.toScore() = Score(
    date = date(dateF ?: dateP),
    value = score.trim().toIntOrNull(),
    max = max,
    description = description,
)

/**
 * Баллы приходят вложенной картой `{предмет: {занятие: [оценки]}}`, пустой ответ — `{}`
 * (`PerformanceService.GetScore` в college-app-core). Предмет запрашивается один, берём первый.
 */
private fun parseScores(body: JsonElement): List<SubjectLesson> = when (body) {
    is JsonObject -> body.values.filterIsInstance<JsonObject>().firstOrNull().orEmpty()
        .map { (lesson, scores) ->
            SubjectLesson(
                title = lesson,
                scores = scores.jsonArray.map { Http.json.decodeFromJsonElement(ScoreDto.serializer(), it).toScore() },
            )
        }
        .sortedBy { it.title }

    else -> emptyList()
}

private fun JsonObject?.orEmpty(): Map<String, JsonElement> = this ?: emptyMap()

private fun date(value: String?): LocalDate? =
    value?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
