package ru.dzhaparidze.mykct.data

/**
 * Справочник групп и подгрупп. Бэкенд каталога не отдаёт вообще — клиент знает его сам
 * (см. ~/Desktop/mykct-android-app-контекст.md, раздел 4).
 *
 * ponytail: обновляется руками каждый сентябрь. Начнёт надоедать — выносить на сервер.
 */
object Groups {

    data class Named(val id: String, val title: String)

    /** ИТ25-11..14, ИТ24-11..14, ИТ23-11..13, ИТ22-11..12. */
    val all: List<String> = listOf(25 to 4, 24 to 4, 23 to 3, 22 to 2)
        .flatMap { (year, count) -> (1..count).map { "ИТ$year-1$it" } }

    /** Те же группы, разложенные по наборам: 2025 → [ИТ25-11..14], порядок от старшего. */
    val bySet: Map<Int, List<String>> = all.groupBy { 2000 + yearOf(it) }

    /** Набор 25 делится на «Подгр1..4», наборы постарше — на профили. */
    fun subgroups(group: String): List<Named> = when (yearOf(group)) {
        25 -> numbered(4)
        else -> PROFILES
    }

    fun englishGroups(group: String): List<String> = ENGLISH[yearOf(group)].orEmpty()

    /** Единственное исключение: у ИТ24-14 профиль CD дополнительно делится пополам. */
    fun profileSubgroups(group: String, subgroup: String?): List<Named> =
        if (group == "ИТ24-14" && subgroup == "CD") numbered(2) else emptyList()

    /** Физкультуру бэкенд отдаёт всегда: фильтр по подгруппе эти три не режет. */
    val sportSubgroups = listOf("ФизраКол", "БрайтФит", "БаскетКол")

    private fun numbered(count: Int) = (1..count).map { Named("Подгр$it", "Подгруппа $it") }

    private fun yearOf(group: String): Int = group.drop(2).take(2).toIntOrNull() ?: 0

    private val PROFILES = listOf(
        Named("BE", "Backend"),
        Named("FE", "Frontend"),
        Named("GD", "Game Dev"),
        Named("PM", "Project Management"),
        Named("SA", "System Administration"),
        Named("CD", "UX/UI Design"),
    )

    private val ENGLISH = mapOf(
        25 to listOf("A0.11", "A0.12", "A1.11", "A1.12", "A2.11", "A2.12", "B1.11", "B1.12"),
        24 to listOf("A0.21", "A1.21", "A1.22", "A1.23", "A2.21", "A2.22", "B1.21", "B1.22"),
        23 to listOf("A1.31", "A2.31", "B1.31"),
        22 to listOf("A1.41", "A2.41", "B1.41"),
    )
}
