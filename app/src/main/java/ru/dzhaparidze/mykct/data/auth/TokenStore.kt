package ru.dzhaparidze.mykct.data.auth

import android.content.Context

/**
 * Refresh-токен между запусками. В iOS это Keychain; на Android приватные
 * SharedPreferences лежат в песочнице приложения и другим приложениям недоступны.
 *
 * ponytail: без шифрования — на рутованном устройстве файл читается.
 * Понадобится защита от физического доступа — EncryptedSharedPreferences.
 */
class TokenStore(context: Context) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var refreshToken: String?
        get() = prefs.getString(REFRESH, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(REFRESH) else putString(REFRESH, value)
        }.apply()

    /** Когда протухает refresh — millis. 0, если неизвестно. */
    var refreshExpiresAt: Long
        get() = prefs.getLong(REFRESH_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(REFRESH_EXPIRES, value).apply()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val REFRESH = "refresh_token"
        const val REFRESH_EXPIRES = "refresh_expires_at"
    }
}
