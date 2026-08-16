package ru.dzhaparidze.mykct.data

import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

interface ScheduleRepository {
    /** Расписание на неделю, начиная с понедельника [monday], для выбора [selection]. */
    suspend fun weekSchedule(monday: LocalDate, selection: Selection): List<Lesson>
}

/**
 * Моки до подключения GET /api/v1/schedule. Сетка звонков и предметы — как в КЦТ,
 * набор пар зависит от дня недели, чтобы точки под датами были разной длины.
 * Часть предметов делится на подгруппы — английский, профильный модуль, физкультура.
 */
class MockScheduleRepository : ScheduleRepository {

    override suspend fun weekSchedule(monday: LocalDate, selection: Selection): List<Lesson> {
        delay(400) // видимая загрузка, чтобы состояние loading не было мёртвым кодом
        return DayOfWeek.values()
            .filter { it != DayOfWeek.SUNDAY }
            .flatMap { dayOfWeek -> lessonsFor(monday.plusDays((dayOfWeek.value - 1).toLong()), dayOfWeek, selection) }
            .mapNotNull { it.filterBy(selection) }
    }

    private fun lessonsFor(date: LocalDate, dayOfWeek: DayOfWeek, selection: Selection): List<Lesson> {
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
                colorHex = subject.colorHex,
                subgroups = subject.split.subgroupsOf(selection.group),
            )
        }
    }

    /**
     * Фильтрация подгрупп — вообще-то работа бэкенда: он режет SubGroup по subgroup /
     * english_group / profile_subgroup и «схлопывает» пару, если осталась ровно одна.
     * Мок повторяет это, чтобы экран увидел обе формы ответа ещё до подключения сети.
     */
    private fun Lesson.filterBy(selection: Selection): Lesson? {
        if (subgroups.isEmpty()) return this
        val kept = subgroups.filter { it.matches(selection) }
        return when (kept.size) {
            0 -> null
            1 -> copy(
                title = kept[0].title,
                topic = topic.ifBlank { kept[0].topic },
                room = room.ifBlank { kept[0].room },
                subgroups = emptyList(),
            )
            else -> copy(subgroups = kept)
        }
    }

    private fun LessonSubgroup.matches(selection: Selection): Boolean = when {
        id in Groups.sportSubgroups -> true
        ENGLISH_ID.matches(id) -> selection.englishGroup == null || selection.englishGroup == id
        // «Подгр1..4» — это либо подгруппа набора 25, либо деление профиля у ИТ24-14
        id.startsWith("Подгр") -> {
            val chosen = selection.subgroup?.takeIf { it.startsWith("Подгр") } ?: selection.profileSubgroup
            chosen == null || chosen == id
        }
        else -> selection.subgroup == null || selection.subgroup == id
    }

    private fun Split.subgroupsOf(group: String): List<LessonSubgroup> = when (this) {
        Split.NONE -> emptyList()
        Split.ENGLISH -> Groups.englishGroups(group).mapIndexed { index, id ->
            LessonSubgroup(id, "Английский язык, $id", "Unit ${index + 1}", "20${index + 1}")
        }
        Split.PROFILE -> Groups.subgroups(group).mapIndexed { index, named ->
            LessonSubgroup(named.id, "Профильный модуль: ${named.title}", "Практика ${index + 1}", "${310 + index}")
        }
        Split.SPORT -> Groups.sportSubgroups.map {
            LessonSubgroup(it, "Физическая культура, $it", "Круговая тренировка", "Спортзал")
        }
    }

    private enum class Split { NONE, ENGLISH, PROFILE, SPORT }

    private data class Subject(
        val title: String,
        val topic: String,
        val room: String,
        val colorHex: String?,
        val split: Split = Split.NONE,
    )

    private companion object {
        val ENGLISH_ID = Regex("""^(A0|A1|A2|B1)\.\d{2}$""")

        val BELLS = listOf(
            LocalTime.of(9, 0) to LocalTime.of(10, 30),
            LocalTime.of(10, 40) to LocalTime.of(12, 10),
            LocalTime.of(12, 40) to LocalTime.of(14, 10),
            LocalTime.of(14, 20) to LocalTime.of(15, 50),
            LocalTime.of(16, 0) to LocalTime.of(17, 30),
        )

        val SUBJECTS = listOf(
            Subject("Разработка модулей ПО", "Слои приложения и внедрение зависимостей", "312", "#6C4CC4"),
            Subject("Английский язык", "", "", "#3882E0", Split.ENGLISH),
            Subject("Базы данных", "Индексы и планы запросов", "218", "#00A884"),
            Subject("Физическая культура", "", "", "#F5A623", Split.SPORT),
            Subject("Операционные системы", "Процессы и планировщик", "305", "#E0575B"),
            Subject("Профильный модуль", "", "", "#8E44AD", Split.PROFILE),
        )
    }
}
