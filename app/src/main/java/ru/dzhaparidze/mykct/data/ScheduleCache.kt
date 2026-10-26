package ru.dzhaparidze.mykct.data

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

/**
 * Последний удачный ответ расписания по каждой неделе и выбору, как в iOS: без сети
 * экран показывает его с плашкой "Нет связи", а не пустую ошибку. Хранится сырой JSON
 * ответа - разбор остаётся один, в [ApiScheduleRepository].
 *
 * Прошедшие недели выбрасываются при каждой записи, поэтому файл не растёт.
 */
class ScheduleCache(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)

    fun get(monday: LocalDate, selection: Selection): String? = prefs.getString(key(monday, selection), null)

    fun put(monday: LocalDate, selection: Selection, json: String, today: LocalDate = LocalDate.now()) {
        val current = today.minusDays(today.dayOfWeek.value - 1L).toString()
        if (monday.toString() < current) return
        prefs.edit {
            prefs.all.keys.filter { it.substringAfterLast('|') < current }.forEach(::remove)
            putString(key(monday, selection), json)
        }
    }

    private fun key(monday: LocalDate, selection: Selection) = listOf(
        selection.group,
        selection.subgroup.orEmpty(),
        selection.englishGroup.orEmpty(),
        selection.profileSubgroup.orEmpty(),
        monday.toString(),
    ).joinToString("|")
}
