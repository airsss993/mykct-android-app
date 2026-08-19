package ru.dzhaparidze.mykct.data.net

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import ru.dzhaparidze.mykct.BuildConfig
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.MockScheduleRepository
import ru.dzhaparidze.mykct.data.Selection
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Боевого сервера пока нет (см. HANDOFF), поэтому debug-сборка по умолчанию ходит
 * не в сеть, а в заглушку: `USE_MOCKS=false` в `local.properties` вернёт настоящий API.
 * Release-сборка этот файл не видит вовсе — у неё свой [httpEngine] с одним OkHttp,
 * так что уехать в RuStore с моками нельзя даже по ошибке.
 */
internal fun httpEngine(): HttpClientEngine =
    if (BuildConfig.USE_MOCKS) mockEngine() else OkHttp.create()

/**
 * Отвечает вместо `college-app-core` и auth-сервиса. Формы ответов — те же, что у
 * настоящего бэкенда (сверено в `tools/local-stand/stub.py`), поэтому разбор в клиенте
 * работает боевой, а не облегчённый.
 */
private fun mockEngine() = MockEngine { request ->
    val path = request.url.encodedPath
    val body = when {
        path.endsWith("/api/v1/schedule") -> schedule(request)
        path.endsWith("/api/v1/classdetails") -> classDetails(request)
        path.endsWith("/api/v1/attendance") -> attendance(request)
        path.endsWith("/api/v1/attendance/streak") -> STREAK
        path.endsWith("/api/v1/performance/subjects") -> subjects()
        path.endsWith("/api/v1/performance/score") -> SCORES
        path.endsWith("/app/signin") -> SIGN_IN
        path.endsWith("/app/access") -> ACCESS
        path.endsWith("/app/refresh") -> REFRESH
        path.endsWith("/app/signout") -> buildJsonObject { put("message", "signed out") }
        else -> null
    }

    if (body == null) {
        respond(
            content = """{"error":"заглушка не знает путь $path"}""",
            status = HttpStatusCode.NotFound,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    } else {
        respond(
            content = body.toString(),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
}

/**
 * Расписание берётся у [MockScheduleRepository] — там уже лежит и сетка звонков, и
 * фильтрация подгрупп, повторяющая бэкенд. Здесь остаётся только собрать её ответ:
 * пары «схлопываются» ещё в репозитории, так что наружу уходят обе формы события.
 */
private suspend fun schedule(request: HttpRequestData): JsonElement {
    val query = request.url.parameters
    val monday = query["start"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val selection = Selection(
        group = query["group"] ?: Selection().group,
        subgroup = query["subgroup"],
        englishGroup = query["english_group"],
        profileSubgroup = query["profile_subgroup"],
    )

    val lessons = MockScheduleRepository().weekSchedule(monday, selection)
    return buildJsonObject {
        putJsonArray("events") {
            lessons.forEach { lesson ->
                add(
                    buildJsonObject {
                        put("ClID", lesson.id)
                        put("Day", lesson.date.toString())
                        put("start", lesson.start.format(TIME))
                        put("end", lesson.end.format(TIME))
                        put("title", lesson.title)
                        put("topic", lesson.topic)
                        put("room", lesson.room)
                        put("group", selection.group)
                        lesson.colorHex?.let { put("color", it) }
                        if (lesson.subgroups.isNotEmpty()) {
                            putJsonArray("SubGroup") {
                                lesson.subgroups.forEachIndexed { index, subgroup ->
                                    add(
                                        buildJsonObject {
                                            put("SClID", "${lesson.id}-$index")
                                            put("SGrID", subgroup.id)
                                            put("SGCaID", subgroup.room)
                                            put("STopic", subgroup.topic)
                                            put("STitle", subgroup.title)
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Портал отдаёт произвольный объект — заглушка тоже, лишь бы формой была карта. */
private fun classDetails(request: HttpRequestData): JsonElement = buildJsonObject {
    put("ClID", request.url.parameters["id"].orEmpty())
    put("teacher", "Иванов И. И.")
    put("building", "Главный корпус")
    put("comment", "Взять ноутбук")
}

/**
 * Посещаемость за ту же неделю, что и расписание: прошедшие пары получают отметку
 * (детерминированно по id — чтобы цифры не прыгали при каждом обновлении),
 * будущие остаются без статуса.
 */
private suspend fun attendance(request: HttpRequestData): JsonElement {
    val start = request.url.parameters["start"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lessons = MockScheduleRepository().weekSchedule(start, Selection())

    return buildJsonArray {
        lessons.forEach { lesson ->
            add(
                buildJsonObject {
                    put("ClID", lesson.id.hashCode().toLong())
                    put("Day", lesson.date.toString())
                    // время здесь бэкенд не обрезает — как в настоящем /attendance
                    put("start", "${lesson.date} ${lesson.start.format(TIME)}")
                    put("end", "${lesson.date} ${lesson.end.format(TIME)}")
                    put("title", lesson.title)
                    put("topic", lesson.topic)
                    put("room", lesson.room)
                    put("status", lesson.status())
                },
            )
        }
    }
}

/** 2 — был, 1 — уважительная, 0 — прогул; будущая пара статуса ещё не имеет. */
private fun Lesson.status(): Int {
    if (date.isAfter(LocalDate.now())) return -1
    return when (Math.floorMod(id.hashCode(), 10)) {
        0 -> 0
        1 -> 1
        else -> 2
    }
}

private suspend fun subjects(): JsonElement {
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val titles = MockScheduleRepository().weekSchedule(monday, Selection()).map { it.title }.distinct()
    return buildJsonArray {
        titles.forEachIndexed { index, title ->
            add(
                buildJsonObject {
                    put("SuID", "su-$index")
                    put("SuIDcrc", "crc-$index")
                    put("Title", title)
                },
            )
        }
    }
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val USER = buildJsonObject {
    put("id", "student01")
    put("username", "student01")
    put("role", "student")
    put("academic_group", "ИТ25-11")
    put("profile", "")
    put("subgroup", "Подгр1")
    put("english_group", "A0.11")
}

private val SIGN_IN: JsonObject = buildJsonObject {
    put("access_token", "mock-access")
    put("refresh_token", "mock-refresh")
    put("access_expires_in", 3600)
    put("refresh_expires_in", 2_592_000)
    put("user", USER)
}

private val ACCESS: JsonObject = buildJsonObject {
    put("access_token", "mock-access")
    put("expires_in", 3600)
    put("user", USER)
}

private val REFRESH: JsonObject = buildJsonObject {
    put("refresh_token", "mock-refresh")
    put("expires_in", 2_592_000)
}

private val STREAK: JsonObject = buildJsonObject {
    put("current_streak", 5)
    put("longest_streak", 12)
    put("total_days_attended", 40)
    put("total_school_days", 45)
    // доля, а не проценты — как в college-app-core; клиент считает процент сам
    put("attendance_rate", 0.888)
    put("last_attended_date", LocalDate.now().minusDays(1).toString())
    put("period_start", LocalDate.now().minusMonths(3).toString())
    put("period_end", LocalDate.now().toString())
}

/** Форма ответа — вложенная карта `{предмет: {занятие: [оценки]}}`, как у бэкенда. */
private val SCORES: JsonObject = buildJsonObject {
    putJsonObject("Предмет") {
        putJsonArray("Практическая работа 1") {
            add(
                buildJsonObject {
                    put("DateF", LocalDate.now().minusDays(7).toString())
                    put("DateP", LocalDate.now().minusDays(7).toString())
                    put("Score", "5")
                    put("MaxScore", 5)
                    put("Description", "Работа на паре")
                },
            )
        }
        putJsonArray("Практическая работа 2") {
            add(
                buildJsonObject {
                    put("DateF", LocalDate.now().toString())
                    put("DateP", "")
                    put("Score", "")
                    put("MaxScore", 5)
                    put("Description", "Не сдано")
                },
            )
        }
    }
}
