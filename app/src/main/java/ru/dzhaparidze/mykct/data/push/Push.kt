package ru.dzhaparidze.mykct.data.push

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.dzhaparidze.mykct.MainActivity
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.api.bearer
import ru.dzhaparidze.mykct.data.auth.AuthService
import ru.dzhaparidze.mykct.data.net.API_URL
import ru.dzhaparidze.mykct.data.net.ApiException
import ru.dzhaparidze.mykct.data.net.Http
import ru.dzhaparidze.mykct.data.net.apiCall
import ru.dzhaparidze.mykct.data.net.ensureSuccess
import java.time.LocalDate

/**
 * Пуши об изменениях расписания через FCM, логика как в iOS (`PushRegistrar`).
 *
 * [sync] сводит регистрацию на сервере к желаемому состоянию: вошёл студент с группой
 * и пуши включены - устройство зарегистрировано, иначе снято. Что ушло на сервер,
 * лежит снимком (токен, логин, группа, время); совпал и моложе недели - запрос не нужен.
 * 4xx снимок сбрасывает без повтора, сеть и 5xx оставляют как есть до следующего запуска.
 *
 * Без google-services.json Firebase не поднимается, и всё здесь молча ничего не делает.
 */
object Push {

    /** Канал уведомлений. Тот же идентификатор шлёт сервер (AndroidChannelID). */
    const val CHANNEL_ID = "schedule"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _enabled = MutableStateFlow(true)
    /** Переключатель "Изменения в расписании" в настройках. */
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private fun available(context: Context) = FirebaseApp.getApps(context).isNotEmpty()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("push", Context.MODE_PRIVATE)

    /** Канал и сохранённый переключатель; зовётся из `MainActivity.onCreate`. */
    fun init(context: Context) {
        _enabled.value = prefs(context).getBoolean(KEY_ENABLED, true)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.push_channel_schedule),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, value) }
        _enabled.value = value
        sync(context)
    }

    /** Свести регистрацию к текущему состоянию. [newToken] - свежий токен из [PushService.onNewToken]. */
    fun sync(context: Context, newToken: String? = null) {
        if (!available(context)) return
        val app = context.applicationContext
        scope.launch { mutex.withLock { runCatching { syncLocked(app, newToken) } } }
    }

    private suspend fun syncLocked(context: Context, newToken: String?) {
        val state = AuthService.get(context).state.value
        if (state.isBootstrapping) return
        val prefs = prefs(context)
        val user = state.user
        val group = user?.academicGroup?.takeIf { it.isNotBlank() }
        val sentToken = prefs.getString(SENT_TOKEN, null)

        if (!_enabled.value || user == null || group == null) {
            if (sentToken == null) return
            // DELETE без Bearer: к этому моменту сессии может уже не быть
            send(prefs) {
                Http.client.delete(DEVICES_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(TokenRequest(sentToken))
                }.ensureSuccess()
                prefs.edit { clear(); putBoolean(KEY_ENABLED, _enabled.value) }
            }
            return
        }

        val token = newToken ?: FirebaseMessaging.getInstance().token.await()
        val fresh = sentToken == token &&
            prefs.getString(SENT_USER, null) == user.id &&
            prefs.getString(SENT_GROUP, null) == group &&
            System.currentTimeMillis() - prefs.getLong(SENT_AT, 0) < TTL_MS
        if (fresh) return

        send(prefs) {
            AuthService.get(context).withToken { access ->
                Http.client.post(DEVICES_URL) {
                    bearer(access)
                    contentType(ContentType.Application.Json)
                    setBody(DeviceRequest(token = token, deviceId = deviceId(context)))
                }.ensureSuccess()
            }
            prefs.edit {
                putString(SENT_TOKEN, token)
                putString(SENT_USER, user.id)
                putString(SENT_GROUP, group)
                putLong(SENT_AT, System.currentTimeMillis())
            }
        }
    }

    /** 4xx - сервер запрос не примет и в следующий раз: снимок сбрасываем, не повторяем. */
    private suspend fun send(prefs: android.content.SharedPreferences, block: suspend () -> Unit) {
        try {
            apiCall { block() }
        } catch (e: ApiException) {
            if (e.status in 400..499) {
                prefs.edit { remove(SENT_TOKEN); remove(SENT_USER); remove(SENT_GROUP); remove(SENT_AT) }
            }
        }
    }

    /**
     * Неделя из пуша: `type` и `week_start` лежат в extras - в фоне их кладёт система
     * в интент запуска, в открытом приложении - [PushService]. Чужой интент даёт null.
     */
    fun weekOf(intent: Intent?): LocalDate? {
        val extras = intent?.extras ?: return null
        if (extras.getString("type") !in TYPES) return null
        return extras.getString("week_start")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    /**
     * Идентификатор установки: сервер держит по нему одну запись, чтобы после смены
     * аккаунта на телефоне пуши не шли обоим студентам.
     */
    @SuppressLint("HardwareIds")
    private fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    private val DEVICES_URL = "$API_URL/notifications/devices"
    private val TYPES = setOf("schedule_published", "schedule_changed")
    private const val TTL_MS = 7L * 24 * 60 * 60 * 1000

    private const val KEY_ENABLED = "enabled"
    private const val SENT_TOKEN = "sent_token"
    private const val SENT_USER = "sent_user"
    private const val SENT_GROUP = "sent_group"
    private const val SENT_AT = "sent_at"

    @Serializable
    private data class DeviceRequest(
        val token: String,
        @SerialName("device_id") val deviceId: String,
        val platform: String = "android",
    )

    @Serializable
    private data class TokenRequest(val token: String)
}

/**
 * Приём пушей. В фоне уведомление рисует сама система по полю notification, сюда
 * пуш попадает, только когда приложение открыто, - тогда показываем его сами.
 */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) = Push.sync(this, token)

    @SuppressLint("MissingPermission") // areNotificationsEnabled проверяет и разрешение
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled() || !Push.enabled.value) return

        // data пуша едет в интент: по тапу MainActivity откроет нужную неделю
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        message.data.forEach { (key, value) -> intent.putExtra(key, value) }
        val built = NotificationCompat.Builder(this, Push.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    message.messageId.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setAutoCancel(true)
            .build()
        manager.notify(message.messageId.hashCode(), built)
    }
}
