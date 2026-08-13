package ru.dzhaparidze.mykct.data

import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

interface ScheduleRepository {
    /** Расписание на неделю, начиная с понедельника [monday]. */
    suspend fun weekSchedule(monday: LocalDate): List<Lesson>
}

/**
 * Моки до подключения GET /api/v1/schedule. Сетка звонков и предметы — как в КЦТ,
 * набор пар зависит от дня недели, чтобы точки под датами были разной длины.
 */
class MockScheduleRepository : ScheduleRepository {

    override suspend fun weekSchedule(monday: LocalDate): List<Lesson> {
        delay(400) // видимая загрузка, чтобы состояние loading не было мёртвым кодом
        return DayOfWeek.values()
            .filter { it != DayOfWeek.SUNDAY }
            .flatMap { dayOfWeek -> lessonsFor(monday.plusDays((dayOfWeek.value - 1).toLong()), dayOfWeek) }
    }

    private fun lessonsFor(date: LocalDate, dayOfWeek: DayOfWeek): List<Lesson> {
        val plan = when (dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(0, 1, 2)
            DayOfWeek.TUESDAY -> listOf(1, 2, 3, 4)
            DayOfWeek.WEDNESDAY -> listOf(0, 1)
            DayOfWeek.THURSDAY -> listOf(0, 1, 2, 3)
            DayOfWeek.FRIDAY -> listOf(2, 3)
            else -> listOf(1)
        }
        return plan.mapIndexed { index, subjectIndex ->
            val (start, end) = BELLS[index]
            val subject = SUBJECTS[(subjectIndex + dayOfWeek.value) % SUBJECTS.size]
            Lesson(
                id = "$date-$index",
                date = date,
                start = start,
                end = end,
                title = subject.title,
                topic = subject.topic,
                room = subject.room,
                subgroup = subject.subgroup,
                colorHex = subject.colorHex,
            )
        }
    }

    private data class Subject(
        val title: String,
        val topic: String,
        val room: String,
        val subgroup: String?,
        val colorHex: String?,
    )

    private companion object {
        val BELLS = listOf(
            LocalTime.of(9, 0) to LocalTime.of(10, 30),
            LocalTime.of(10, 40) to LocalTime.of(12, 10),
            LocalTime.of(12, 40) to LocalTime.of(14, 10),
            LocalTime.of(14, 20) to LocalTime.of(15, 50),
            LocalTime.of(16, 0) to LocalTime.of(17, 30),
        )

        val SUBJECTS = listOf(
            Subject("Разработка модулей ПО", "Слои приложения и внедрение зависимостей", "312", "BE", "#6C4CC4"),
            Subject("Английский язык", "Present Perfect vs Past Simple", "204", "B1.21", "#3882E0"),
            Subject("Базы данных", "Индексы и планы запросов", "218", null, "#00A884"),
            Subject("Физическая культура", "Круговая тренировка", "Спортзал", "ФизраКол", "#F5A623"),
            Subject("Операционные системы", "Процессы и планировщик", "305", "SA", "#E0575B"),
            Subject("Проектирование интерфейсов", "Сетки и типографика", "210", "CD", "#8E44AD"),
        )
    }
}
