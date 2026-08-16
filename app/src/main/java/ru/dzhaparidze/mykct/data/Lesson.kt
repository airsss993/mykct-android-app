package ru.dzhaparidze.mykct.data

import java.time.LocalDate
import java.time.LocalTime

/**
 * Пара в расписании. Поля — то, что реально отдаёт GET /api/v1/schedule
 * (см. ~/Desktop/mykct-android-app-контекст.md), уже нормализованное:
 * ClID -> id, Day -> date, start/end -> LocalTime.
 */
data class Lesson(
    val id: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val title: String,
    val topic: String,
    val room: String,
    val colorHex: String?,
    /**
     * Пустой, если пара общая для группы или если бэкенд «схлопнул» единственную
     * оставшуюся подгруппу в поля самой пары. Иначе — то, между чем делится группа.
     */
    val subgroups: List<LessonSubgroup> = emptyList(),
)

/** Элемент SubGroup: SGrID -> id, STitle -> title, STopic -> topic, SGCaID -> room. */
data class LessonSubgroup(
    val id: String,
    val title: String,
    val topic: String,
    val room: String,
)
