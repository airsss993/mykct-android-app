package ru.dzhaparidze.mykct.data

import android.content.Context

/** Тема приложения. Перенесено из версии 1.1.1 — пользователи к этому выбору привыкли. */
enum class ThemeMode(val title: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная"),
}

/** Одна строка в SharedPreferences — как и выбор группы, DataStore тут не за что. */
class ThemeStore(context: Context) {

    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    fun load(): ThemeMode = prefs.getString(MODE, null)
        ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
        ?: ThemeMode.SYSTEM

    fun save(mode: ThemeMode) = prefs.edit().putString(MODE, mode.name).apply()

    private companion object {
        const val MODE = "mode"
    }
}
