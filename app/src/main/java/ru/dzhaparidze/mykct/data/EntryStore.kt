package ru.dzhaparidze.mykct.data

import android.content.Context

/**
 * Прошёл ли пользователь экран входа. Одна булева строка в SharedPreferences —
 * DataStore тут не за что, как и в [ThemeStore].
 */
class EntryStore(context: Context) {

    private val prefs = context.getSharedPreferences("entry", Context.MODE_PRIVATE)

    fun passed(): Boolean = prefs.getBoolean(PASSED, false)

    fun markPassed() = prefs.edit().putBoolean(PASSED, true).apply()

    private companion object {
        const val PASSED = "passed"
    }
}
