package ru.dzhaparidze.mykct.data

/**
 * Разбиение пары на самостоятельные занятия — порт `LessonSplitting` из iOS.
 *
 * Портал отдаёт три формы: пара общая, пара делится (и своя подгруппа там одна из
 * многих), и пара, все подгруппы которой — свои. Последнее и есть параллельные занятия:
 * у студента в один слот идут два-три разных предмета. Одной карточкой их не показать,
 * поэтому неделя разворачивается сразу после загрузки, до сборки UI: дальше таймлайн
 * работает с обычными парами и не знает, что когда-то они были одной.
 */
private const val MAX_PARALLEL = 3

/** То, что студент выбрал в шапке: подгруппа, английская группа, подгруппа профиля. */
internal fun selectedIds(selection: Selection): Set<String> =
    setOfNotNull(selection.subgroup, selection.englishGroup, selection.profileSubgroup)
        .filter { it.isNotBlank() && it != "*" }
        .toSet()

fun List<Lesson>.splitOwn(selection: Selection): List<Lesson> {
    val ids = selectedIds(selection)
    return flatMap { lesson ->
        if (lesson.isOwn(ids)) lesson.subgroups.map { lesson.withSubgroup(it) } else listOf(lesson)
    }
}

/**
 * Ограничение 2..3 — от iOS: у английского подгрупп четыре и своя там ровно одна,
 * так что «все подгруппы свои» на нём не срабатывает, а пара с двумя-тремя своими
 * подгруппами — это как раз параллель.
 */
private fun Lesson.isOwn(ids: Set<String>): Boolean =
    subgroups.size in 2..MAX_PARALLEL && ids.isNotEmpty() && subgroups.all { it.id in ids }

internal fun Lesson.withSubgroup(subgroup: LessonSubgroup): Lesson = copy(
    // SClID, иначе детали у всех получившихся пар будут одни на всех
    id = subgroup.classId.ifBlank { "$id-${subgroup.id}" },
    title = subgroup.title.ifBlank { title },
    topic = subgroup.topic.ifBlank { topic },
    room = subgroup.room.ifBlank { room },
    // иначе на карточке снова «Подгруппы: 2», хотя подгруппа тут уже одна
    subgroups = emptyList(),
)
