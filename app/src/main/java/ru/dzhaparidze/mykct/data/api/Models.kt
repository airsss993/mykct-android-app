package ru.dzhaparidze.mykct.data.api

import java.time.LocalDate
import java.time.LocalTime

/** Отметка на паре. `status == 2` — был, остальное — нет (см. контракт API). */
enum class Attendance(val title: String) {
    PRESENT("Был"),
    ABSENT("Не был (Н/У)"),
    EXCUSED("Не был (Ув.)"),
    UNKNOWN("Неизвестно");

    companion object {
        fun of(status: Int) = when (status) {
            2 -> PRESENT
            1 -> EXCUSED
            0 -> ABSENT
            else -> UNKNOWN
        }
    }
}

/** Пара из /attendance. Отдельная модель, а не [ru.dzhaparidze.mykct.data.Lesson]:
 *  у этого эндпоинта другие типы полей и другая структура подгрупп. */
data class AttendanceRecord(
    val id: String,
    val date: LocalDate,
    val start: LocalTime?,
    val end: LocalTime?,
    val title: String,
    val topic: String,
    val room: String,
    val attendance: Attendance,
)

/** Сводка по загруженному периоду — считается на клиенте, бэкенд её не отдаёт. */
data class AttendanceStats(
    val total: Int,
    val present: Int,
    val absent: Int,
    val excused: Int,
) {
    val percent: Int = if (total > 0) present * 100 / total else 0

    companion object {
        /**
         * Пары без отметки в счёт не идут. Они приходят у будущих занятий
         * (`status == -1`), и на месячном периоде тянули процент вниз вдвое:
         * 10 сентября выходило "30%, был на 21 из 70 пар" - 44 из них ещё не были.
         */
        fun of(records: List<AttendanceRecord>): AttendanceStats {
            val marked = records.filter { it.attendance != Attendance.UNKNOWN }
            return AttendanceStats(
                total = marked.size,
                present = marked.count { it.attendance == Attendance.PRESENT },
                absent = marked.count { it.attendance == Attendance.ABSENT },
                excused = marked.count { it.attendance == Attendance.EXCUSED },
            )
        }
    }
}

/** Стрик посещаемости с 1 сентября текущего учебного года. */
data class Streak(
    val current: Int,
    val longest: Int,
    val daysAttended: Int,
    val schoolDays: Int,
    val rate: Double,
    val lastAttended: LocalDate?,
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
)

data class Subject(val id: String, val title: String)

/** Баллы по предмету: занятие → его оценки. */
data class SubjectLesson(val title: String, val scores: List<Score>)

data class Score(
    val date: LocalDate?,
    val value: Int?,
    val max: Int,
    val description: String,
) {
    val isGraded: Boolean get() = value != null
}
