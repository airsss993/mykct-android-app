package ru.dzhaparidze.mykct.feature.schedule

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.slotCount
import ru.dzhaparidze.mykct.feature.schedule.components.remainingText
import ru.dzhaparidze.mykct.feature.schedule.components.secondsLeft

class LessonProgressTest {

    private val lesson = Lesson(
        id = "cl-1",
        date = LocalDate.of(2026, 8, 10),
        start = LocalTime.of(9, 0),
        end = LocalTime.of(10, 0),
        title = "Математика",
        topic = "",
        room = "201",
        colorHex = null,
    )

    @Test
    fun `остаток округляется вверх`() {
        // 40 минут 30 секунд до конца — на карточке должна быть 41-я минута, а не 40-я
        assertEquals(2430, secondsLeft(lesson, LocalTime.of(9, 19, 30)))
        assertEquals("40 мин", remainingText(2430))
    }

    @Test
    fun `последняя минута идёт в секундах`() {
        assertEquals("45 с", remainingText(secondsLeft(lesson, LocalTime.of(9, 59, 15))))
    }

    @Test
    fun `после конца пары остатка нет`() {
        assertEquals(0, secondsLeft(lesson, LocalTime.of(10, 30)))
    }

    @Test
    fun `параллельные подгруппы считаются одной парой`() {
        val parallel = lesson.copy(id = "cl-2", title = "Английский")
        val next = lesson.copy(id = "cl-3", start = LocalTime.of(10, 10), end = LocalTime.of(11, 40))
        assertEquals(2, listOf(lesson, parallel, next).slotCount())
    }
}
