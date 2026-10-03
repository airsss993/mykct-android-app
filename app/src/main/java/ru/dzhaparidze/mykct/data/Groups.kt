package ru.dzhaparidze.mykct.data

/**
 * Справочник групп и подгрупп. Бэкенд каталога не отдаёт вообще - клиент знает его сам
 * (см. ~/Desktop/mykct-android-app-контекст.md, раздел 4).
 *
 * ponytail: обновляется руками каждый сентябрь. Начнёт надоедать - выносить на сервер.
 */
object Groups {

    data class Named(val id: String, val title: String)

    /** Год набора первого курса. Меняется каждый сентябрь вместе со списком ниже. */
    private const val FIRST_YEAR = 26

    /** ИТ26-11..14, ИТ25-11..14, ИТ24-11..14, ИТ23-11..13 - как в iOS. */
    val all: List<String> = listOf(26 to 4, 25 to 4, 24 to 4, 23 to 3)
        .flatMap { (year, count) -> (1..count).map { "ИТ$year-1$it" } }

    /** Те же группы, разложенные по наборам: 2026 -> [ИТ26-11..14], порядок от младшего курса. */
    val bySet: Map<Int, List<String>> = all.groupBy { 2000 + (it.drop(2).take(2).toIntOrNull() ?: 0) }

    /** Первый курс делится на "Подгр1..2", старшие - на профили. */
    fun subgroups(group: String): List<Named> = if (course(group) == 1) numbered(2) else PROFILES

    fun englishGroups(group: String): List<String> = course(group)?.let { ENGLISH[it] }.orEmpty()

    /** У старших курсов профили FE и CD дополнительно делятся пополам. */
    fun profileSubgroups(group: String, subgroup: String?): List<Named> {
        val course = course(group) ?: return emptyList()
        return if (course > 1 && subgroup in PROFILES_WITH_SUBGROUPS) numbered(2) else emptyList()
    }

    /** Физкультуру бэкенд отдаёт всегда: фильтр по подгруппе эти три не режет. */
    val sportSubgroups = listOf("ФизраКол", "БрайтФит", "БаскетКол")

    private fun numbered(count: Int) = (1..count).map { Named("Подгр$it", "Подгруппа $it") }

    /** Курс 1..4 по году набора; null - выпустились или опечатка. */
    private fun course(group: String): Int? {
        val year = group.drop(2).take(2).toIntOrNull() ?: return null
        return (FIRST_YEAR - year + 1).takeIf { it in ENGLISH }
    }

    private val PROFILES = listOf(
        Named("BE", "Backend"),
        Named("FE", "Frontend"),
        Named("GD", "Game Dev"),
        Named("PM", "Project Management"),
        Named("SA", "System Administration"),
        Named("CD", "UX/UI Design"),
    )

    private val PROFILES_WITH_SUBGROUPS = setOf("FE", "CD")

    private val ENGLISH = mapOf(
        1 to listOf("A0.11", "A0.12", "A1.11", "A1.12", "A2.11", "A2.12", "B1.11", "B1.12"),
        2 to listOf("A0.21", "A0.22", "A1.21", "A1.22", "A2.21", "A2.22", "B1.21", "B1.22"),
        3 to listOf("A1.31", "A2.31", "B1.31", "B2.31"),
        4 to listOf("A1.41", "A2.41", "B1.41"),
    )
}
