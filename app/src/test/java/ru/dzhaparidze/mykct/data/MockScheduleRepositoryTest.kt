package ru.dzhaparidze.mykct.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MockScheduleRepositoryTest {

    private val monday = LocalDate.of(2026, 8, 10) // понедельник

    @Test
    fun `неделя укладывается в понедельник-субботу`() = runBlocking {
        val lessons = MockScheduleRepository().weekSchedule(monday)

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
    fun `пары внутри дня идут подряд и не накладываются`() = runBlocking {
        val byDate = MockScheduleRepository().weekSchedule(monday).groupBy { it.date }

        byDate.forEach { (date, lessons) ->
            val sorted = lessons.sortedBy { it.start }
            sorted.forEach { assertTrue("$date: конец раньше начала", it.end.isAfter(it.start)) }
            sorted.zipWithNext { previous, next ->
                assertTrue("$date: пары накладываются", !next.start.isBefore(previous.end))
            }
        }
    }

    @Test
    fun `количество пар в день совпадает с количеством уникальных id`() = runBlocking {
        val lessons = MockScheduleRepository().weekSchedule(monday)

        assertEquals(lessons.size, lessons.map { it.id }.toSet().size)
    }
}
