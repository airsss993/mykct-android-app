package ru.dzhaparidze.mykct.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.dzhaparidze.mykct.data.api.Attendance
import ru.dzhaparidze.mykct.data.api.AttendanceRecord
import ru.dzhaparidze.mykct.data.api.AttendanceStats
import ru.dzhaparidze.mykct.data.auth.User
import java.time.LocalDate
import java.time.LocalTime

/** Разбор того, что приходит от портала, и перенос профиля студента в выбор группы. */
class ApiMappingTest {

    @Test
    fun `время разбирается во всех трёх видах, что шлёт портал`() {
        assertEquals(LocalTime.of(9, 0), parseTime("09:00"))
        assertEquals(LocalTime.of(9, 0), parseTime("9:00"))
        // /attendance отдаёт время целой датой
        assertEquals(LocalTime.of(15, 40), parseTime("2026-09-01 15:40"))
        assertNull(parseTime(""))
        assertNull(parseTime("—"))
    }

    @Test
    fun `детали пары складываются в плоский список без того, что уже на карточке`() {
        val json = """
            {"ClID": "12345", "teacher": "Иванов И.И.", "title": "Математика",
             "room": "", "note": null, "any": {"nested": true, "empty": ""},
             "files": [{"name": "лекция.pdf"}, {"name": "задание.pdf"}]}
        """.trimIndent()

        assertEquals(
            listOf(
                "teacher" to "Иванов И.И.",
                "nested" to "true",
                "name" to "лекция.pdf",
                "name" to "задание.pdf",
            ),
            Json.parseToJsonElement(json).jsonObject.flattenDetails(),
        )
    }

    @Test
    fun `первокурснику подгруппа берётся из subgroup, старшему — из профиля`() {
        val first = selectionOf(
            User(academicGroup = "ИТ25-11", subgroup = "Подгр2", profile = "BE", englishGroup = "A1.11"),
        )
        assertEquals("ИТ25-11", first?.group)
        assertEquals("Подгр2", first?.subgroup)
        assertEquals("A1.11", first?.englishGroup)
        assertNull(first?.profileSubgroup)

        val senior = selectionOf(
            User(academicGroup = "ИТ23-11", subgroup = "Подгр1", profile = "BE", englishGroup = "A1.31"),
        )
        assertEquals("BE", senior?.subgroup)
        // «Подгр1» у старших курсов в подгруппу не лезет: там профили
        assertNull(senior?.profileSubgroup)
    }

    @Test
    fun `у ИТ24-14 с профилем CD подгруппа уходит в profile_subgroup`() {
        val selection = selectionOf(
            User(academicGroup = "ИТ24-14", subgroup = "Подгр2", profile = "CD"),
        )
        assertEquals("CD", selection?.subgroup)
        assertEquals("Подгр2", selection?.profileSubgroup)
    }

    @Test
    fun `группы вне справочника дают null, а не чужое расписание`() {
        assertNull(selectionOf(User(academicGroup = "ИТ19-11")))
        assertNull(selectionOf(User(academicGroup = null)))
    }

    @Test
    fun `статусы посещаемости считаются по правилу «2 — был»`() {
        val stats = AttendanceStats.of(
            listOf(record(2), record(2), record(1), record(0), record(7)),
        )
        assertEquals(5, stats.total)
        assertEquals(2, stats.present)
        assertEquals(1, stats.excused)
        assertEquals(1, stats.absent)
        assertEquals(40, stats.percent)
    }

    @Test
    fun `пустой период не делит на ноль`() {
        assertEquals(0, AttendanceStats.of(emptyList()).percent)
    }

    private fun record(status: Int) = AttendanceRecord(
        id = "1",
        date = LocalDate.of(2026, 9, 1),
        start = LocalTime.of(9, 0),
        end = LocalTime.of(10, 30),
        title = "Пара",
        topic = "",
        room = "301",
        attendance = Attendance.of(status),
    )
}
