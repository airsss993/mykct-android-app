package ru.dzhaparidze.mykct.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.dzhaparidze.mykct.data.net.ApiException

/** Что знает приложение о студенте прямо сейчас. */
data class AuthState(
    val user: User? = null,
    val isBootstrapping: Boolean = true,
) {
    val isAuthenticated: Boolean get() = user != null
}

/**
 * Сессия студента: пара токенов, автологин при запуске и обновление access-токена.
 * Логика снята с iOS (`AuthService.swift` + `AuthSession.swift`):
 *
 * - refresh-токен живёт долго и лежит в [TokenStore]; access — только в памяти;
 * - access обновляется за минуту до протухания, refresh — за неделю;
 * - на 401 запрос повторяется один раз со свежим access (см. [withToken]).
 *
 * Обновление под [mutex]: иначе три экрана, стартующие одновременно, сделают три
 * запроса на access и два из них получат уже отозванный refresh.
 */
class AuthService(context: Context, private val api: AuthApi = AuthApi()) {

    private val tokens = TokenStore(context.applicationContext)
    private val mutex = Mutex()

    private var accessToken: String? = null
    private var accessExpiresAt: Long = 0L

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Есть ли сохранённый вход — до всякой сети. */
    fun hasStoredSession(): Boolean = tokens.refreshToken != null

    suspend fun signIn(username: String, password: String) {
        val response = api.signIn(username, password)
        tokens.refreshToken = response.refreshToken
        tokens.refreshExpiresAt = now() + response.refreshExpiresIn * 1000
        accessToken = response.accessToken
        accessExpiresAt = now() + response.accessExpiresIn * 1000
        _state.value = AuthState(user = response.user, isBootstrapping = false)
    }

    /** Автологин при старте: молча — если не вышло, приложение просто без входа. */
    suspend fun bootstrap() {
        if (tokens.refreshToken == null) {
            _state.value = AuthState(user = null, isBootstrapping = false)
            return
        }
        try {
            refreshAccess()
        } catch (e: ApiException) {
            // Протухший или отозванный refresh — чистим, чтобы не долбить сервер каждый старт
            if (e.status == 401 || e.status == 403) logoutLocal()
        }
        _state.value = _state.value.copy(isBootstrapping = false)
    }

    suspend fun signOut() {
        tokens.refreshToken?.let { runCatching { api.signOut(it) } }
        logoutLocal()
    }

    /**
     * Выполняет запрос с Bearer-токеном; на 401 обновляет токен и повторяет один раз.
     * Второй 401 — это уже не «протух», а «нет доступа», повторять нечего.
     */
    suspend fun <T> withToken(block: suspend (String) -> T): T {
        val token = validAccessToken()
        return try {
            block(token)
        } catch (e: ApiException) {
            if (e.status != 401) throw e
            accessExpiresAt = 0L
            block(validAccessToken())
        }
    }

    private suspend fun validAccessToken(): String = mutex.withLock {
        val current = accessToken
        if (current != null && now() + ACCESS_LEEWAY < accessExpiresAt) return current
        refreshAccess()
        accessToken ?: throw ApiException(401, "Требуется авторизация")
    }

    /** Обновляет access по refresh, а сам refresh — если ему осталось меньше недели. */
    private suspend fun refreshAccess() {
        val refresh = tokens.refreshToken ?: throw ApiException(401, "Требуется авторизация")

        val stored = tokens.refreshExpiresAt
        val rotated = if (stored != 0L && now() + REFRESH_LEEWAY >= stored) {
            runCatching { api.refreshRefreshToken(refresh) }.getOrNull()?.also {
                tokens.refreshToken = it.refreshToken
                tokens.refreshExpiresAt = now() + it.expiresIn * 1000
            }?.refreshToken
        } else {
            null
        }

        val response = api.accessToken(rotated ?: refresh)
        accessToken = response.accessToken
        accessExpiresAt = now() + response.expiresIn * 1000
        _state.value = _state.value.copy(user = response.user)
    }

    private fun logoutLocal() {
        tokens.clear()
        accessToken = null
        accessExpiresAt = 0L
        _state.value = AuthState(user = null, isBootstrapping = false)
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        /** Сессия одна на приложение: DI-фреймворка тут нет, а второй экземпляр обновлял бы токен параллельно. */
        @Volatile private var instance: AuthService? = null

        fun get(context: Context): AuthService = instance ?: synchronized(this) {
            instance ?: AuthService(context).also { instance = it }
        }

        private const val ACCESS_LEEWAY = 60_000L            // минута до протухания access
        private const val REFRESH_LEEWAY = 7L * 24 * 3600_000 // неделя до протухания refresh
    }
}
