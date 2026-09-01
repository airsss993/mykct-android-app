package ru.dzhaparidze.mykct.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MockScheduleRepositoryTest {

    private val monday = LocalDate.of(2026, 8, 10) // понедельник
    private val repository = MockScheduleRepository()

    private fun week(selection: Selection = Selection()) = runBlocking {
        repository.weekSchedule(monday, selection).lessons
    }

    @Test
    fun `неделя укладывается в понедельник-субботу`() {
        val lessons = week()

        assertTrue(lessons.isNotEmpty())
        lessons.forEach { lesson ->
            assertTrue(
                "пара вне недели: ${lesson.date}",
                !lesson.date.isBefore(monday) && !lesson.date.isAfter(monday.plusDays(6)),
            )
            assertTrue("воскресенье не учебное", lesson.date.dayOfWeek != DayOfWeek.SUNDAY)
        }
    }

    @Test
    fun `пары внутри дня идут подряд и не накладываются`() {
        week().groupBy { it.date }.forEach { (date, lessons) ->
            val sorted = lessons.sortedBy { it.start }
            sorted.forEach { assertTrue("$date: конец раньше начала", it.end.isAfter(it.start)) }
            sorted.zipWithNext { previous, next ->
                assertTrue("$date: пары накладываются", !next.start.isBefore(previous.end))
            }
        }
    }

    @Test
    fun `количество пар в день совпадает с количеством уникальных id`() {
        val lessons = week()

        assertEquals(lessons.size, lessons.map { it.id }.toSet().size)
    }

    @Test
    fun `без выбора подгруппы пара отдаётся со всеми подгруппами`() {
        val english = week().first { it.subgroups.any { sub -> sub.id.startsWith("A") || sub.id.startsWith("B") } }

        assertEquals(Groups.englishGroups(Selection().group).size, english.subgroups.size)
    }

    @Test
    fun `выбранная подгруппа схлопывается в саму пару`() {
        val selection = Selection(group = "ИТ24-11", subgroup = "BE")

        val profile = week(selection).first { it.title.startsWith("Профильный модуль") }

        assertTrue("подгруппа осталась массивом", profile.subgroups.isEmpty())
        assertEquals("Профильный модуль: Backend", profile.title)
    }

    @Test
    fun `физкультуру фильтр по подгруппе не режет`() {
        val lessons = week(Selection(group = "ИТ24-11", subgroup = "BE", englishGroup = "A1.21"))

        val sport = lessons.first { it.subgroups.any { sub -> sub.id in Groups.sportSubgroups } }
        assertEquals(Groups.sportSubgroups.size, sport.subgroups.size)
    }

    @Test
    fun `выбор английской группы оставляет одну`() {
        val lessons = week(Selection(group = "ИТ24-11", englishGroup = "A1.21"))

        assertTrue(
            "английский не схлопнулся",
            lessons.any { it.title == "Английский язык, A1.21" && it.subgroups.isEmpty() },
        )
    }
}
