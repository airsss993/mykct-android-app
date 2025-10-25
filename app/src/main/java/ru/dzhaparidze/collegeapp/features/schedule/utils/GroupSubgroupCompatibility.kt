package ru.dzhaparidze.collegeapp.features.schedule.utils

object GroupSubgroupCompatibility {
    private val firstYearSubgroups = listOf("Подгр1", "Подгр2", "Подгр3", "Подгр4")
    private val profileSubgroups = listOf("Подгр1", "Подгр2")
    private val profiles = listOf("BE", "FE", "GD", "PM", "SA", "CD")
    private val englishByYear = mapOf(
        "25" to listOf("A0.11", "A0.12", "A1.11", "A1.12", "A2.11", "A2.12", "B1.11", "B1.12"),
        "24" to listOf("A0.21", "A1.21", "A1.22", "A1.23", "A2.21", "A2.22", "B1.21", "B1.22"),
        "23" to listOf("A1.31", "A2.31", "B1.31"),
        "22" to listOf("A1.41", "A2.41", "B1.41")
    )

    fun getMainSubgroups(group: String): List<String> {
        val year = getYear(group) ?: return emptyList()

        return when (year) {
            "25" -> firstYearSubgroups
            "24", "23", "22" -> profiles
            else -> emptyList()
        }
    }

    fun getEnglishSubgroups(group: String): List<String> {
        val year = getYear(group) ?: return emptyList()
        return englishByYear[year] ?: emptyList()
    }

    fun getAdditionalSubgroups(group: String, selectedMainSubgroup: String): List<String> {
        return when {
            group == "ИТ24-14" && selectedMainSubgroup == "CD" -> profileSubgroups
            else -> emptyList()
        }
    }

    fun getYear(group: String): String? {
        val components = group.split(Regex("[^0-9]")).filter { it.isNotEmpty() }
        return components.firstOrNull { it.length == 2 }
    }
}
