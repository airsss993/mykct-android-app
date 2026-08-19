package ru.dzhaparidze.mykct.feature.schedule

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.dzhaparidze.mykct.data.ScheduleSettings
import ru.dzhaparidze.mykct.data.ScheduleView
import java.time.LocalDate

/** Понедельник 17 августа 2026 — та же неделя, что на скриншотах проверки. */
private val MONDAY: LocalDate = LocalDate.of(2026, 8, 17)

private fun days(settings: ScheduleSettings) = weekDays(MONDAY, settings)

class VisibleDaysTest {

    @Test
    fun `выходные скрыты — в полоске недели пять дней`() {
        val settings = ScheduleSettings(skipWeekends = true)
        assertEquals(5, days(settings).size)
        assertEquals(MONDAY.plusDays(4), days(settings).last())
    }

    @Test
    fun `вид «сегодня» показывает один выбранный день`() {
        val settings = ScheduleSettings(view = ScheduleView.TODAY)
        val selected = MONDAY.plusDays(2)
        assertEquals(listOf(selected), visibleDays(days(settings), selected, settings))
    }

    @Test
    fun `вид «три дня» упирается в конец недели, а не тянет следующую`() {
        val settings = ScheduleSettings(view = ScheduleView.THREE_DAYS)
        val visible = visibleDays(days(settings), MONDAY.plusDays(6), settings)
        assertEquals(listOf(MONDAY.plusDays(6)), visible)
    }

    @Test
    fun `выбран скрытый выходной — показываем хвост рабочей недели`() {
        val settings = ScheduleSettings(view = ScheduleView.THREE_DAYS, skipWeekends = true)
        val visible = visibleDays(days(settings), MONDAY.plusDays(6), settings)
        assertEquals(listOf(MONDAY.plusDays(2), MONDAY.plusDays(3), MONDAY.plusDays(4)), visible)
    }

    @Test
    fun `вид «неделя» показывает всё, что осталось после фильтра выходных`() {
        val settings = ScheduleSettings(view = ScheduleView.WEEK, skipWeekends = true)
        assertEquals(days(settings), visibleDays(days(settings), MONDAY.plusDays(3), settings))
    }
}
