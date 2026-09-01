package ru.dzhaparidze.mykct.data

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class LessonSplittingTest {

    private fun lesson(vararg subgroups: LessonSubgroup) = Lesson(
        id = "cl-1",
        date = LocalDate.of(2026, 8, 10),
        start = LocalTime.of(9, 0),
        end = LocalTime.of(10, 30),
        title = "Английский язык",
        topic = "Тема пары",
        room = "301",
        colorHex = null,
        subgroups = subgroups.toList(),
    )

    private fun subgroup(id: String, title: String = "", room: String = "") =
        LessonSubgroup(id = id, title = title, topic = "", room = room, classId = "s-$id")

    @Test
    fun `пара, где все подгруппы свои, разбивается на отдельные`() {
        val selection = Selection(subgroup = "Подгр1", englishGroup = "A0.11")
        val week = listOf(lesson(subgroup("Подгр1", "Математика", "201"), subgroup("A0.11", "Английский", "305")))

        val result = week.splitOwn(selection)

        assertEquals(listOf("Математика", "Английский"), result.map { it.title })
        assertEquals(listOf("201", "305"), result.map { it.room })
        // детали у каждой свои — иначе обе покажут одно и то же занятие
        assertEquals(listOf("s-Подгр1", "s-A0.11"), result.map { it.id })
        assertEquals(listOf(0, 0), result.map { it.subgroups.size })
    }

    @Test
    fun `пара с чужой подгруппой остаётся одной карточкой`() {
        val selection = Selection(subgroup = "Подгр1")
        val week = listOf(lesson(subgroup("Подгр1"), subgroup("Подгр2")))

        assertEquals(1, week.splitOwn(selection).size)
    }

    @Test
    fun `без выбора подгрупп ничего не разбивается`() {
        val week = listOf(lesson(subgroup("Подгр1"), subgroup("Подгр2")))

        assertEquals(week, week.splitOwn(Selection()))
    }

    @Test
    fun `заголовок склеивает названия подгрупп`() {
        val split = lesson(subgroup("Подгр1", "Математика"), subgroup("Подгр2", "Физика"))

        assertEquals("Математика · Физика", split.displayTitle)
    }

    @Test
    fun `у пары с одним названием заголовок не меняется`() {
        val same = lesson(subgroup("Подгр1", "Английский язык"), subgroup("Подгр2", "Английский язык"))

        assertEquals("Английский язык", same.displayTitle)
    }
}
