package ru.dzhaparidze.mykct.feature.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.dzhaparidze.mykct.data.Lesson
import java.time.LocalDate
import java.time.LocalTime

class WidgetDayTest {

    private val today = LocalDate.of(2026, 10, 13)

    private fun lesson(date: LocalDate, start: Int, end: Int) = Lesson(
        id = "$date-$start", date = date,
        start = LocalTime.of(start, 0), end = LocalTime.of(end, 0),
        title = "Пара", topic = "", room = "", colorHex = null,
    )

    private val week = listOf(
        lesson(today, 11, 12), lesson(today, 9, 10),
        lesson(today.plusDays(1), 10, 11),
    )

    @Test
    fun `идущая пара первая, прошедшие отброшены`() {
        val day = pickDay(week, today.atTime(9, 30))
        assertTrue(day.isToday)
        assertEquals(2, day.slots)
        assertEquals(listOf(9, 11), day.lessons.map { it.start.hour })
        assertEquals(today.atTime(10, 0), day.nextChange(today.atTime(9, 30)))
    }

    @Test
    fun `после последней пары - завтра, перерисовка в полночь`() {
        val day = pickDay(week, today.atTime(12, 0))
        assertFalse(day.isToday)
        assertEquals(listOf(10), day.lessons.map { it.start.hour })
        assertEquals(today.plusDays(1).atStartOfDay(), day.nextChange(today.atTime(12, 0)))
    }

    @Test
    fun `в перерыве ждём начала следующей пары`() {
        val now = today.atTime(10, 30)
        assertEquals(today.atTime(11, 0), pickDay(week, now).nextChange(now))
    }
}
