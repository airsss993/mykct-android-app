package ru.dzhaparidze.collegeapp.features.schedule.utils

fun getFullSubgroupName(shortName: String): String {
    return when (shortName.uppercase()) {
        "BE" -> "Backend (BE)"
        "GD" -> "Game Dev (GD)"
        "PM" -> "Project Management (PM)"
        "SA" -> "System Administration (SA)"
        "FE" -> "Frontend (FE)"
        "CD" -> "UX/UI Design (CD)"
        else -> shortName
    }
}