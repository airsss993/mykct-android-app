package ru.dzhaparidze.mykct

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import android.content.Intent
import java.time.LocalDate
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dzhaparidze.mykct.data.EntryStore
import ru.dzhaparidze.mykct.data.auth.AuthService
import ru.dzhaparidze.mykct.data.ThemeMode
import ru.dzhaparidze.mykct.data.ThemeStore
import ru.dzhaparidze.mykct.data.push.Push
import ru.dzhaparidze.mykct.feature.AppShell
import ru.dzhaparidze.mykct.feature.auth.AuthScreen
import ru.dzhaparidze.mykct.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    /** Неделя из тапнутого пуша; AppShell откроет её и обнулит. */
    private var pushWeek by mutableStateOf<LocalDate?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Push.weekOf(intent)?.let { pushWeek = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Push.init(this)
        // После поворота интент тот же - второй раз неделю не открываем
        if (savedInstanceState == null) pushWeek = Push.weekOf(intent)
        setContent {
            val store = remember { ThemeStore(this) }
            var themeMode by remember { mutableStateOf(store.load()) }
            // Экран входа показываем один раз — пока его не прошли (входом или «без входа»).
            val entryStore = remember { EntryStore(this) }
            val auth = remember { AuthService.get(this) }
            // Вошедшего приветственный экран уже не касается, даже если он его не проходил
            var entered by remember { mutableStateOf(entryStore.passed() || auth.hasStoredSession()) }

            // Автологин: обменять сохранённый refresh на access. Молча — не вышло,
            // значит приложение работает без входа, расписание от токена не зависит.
            LaunchedEffect(Unit) { auth.bootstrap() }

            // Пуши: регистрация сводится к состоянию на каждом его изменении - вход, выход,
            // смена группы, переключатель. Разрешение на Android 13+ спрашиваем после входа:
            // без входа пушам некуда приходить, и просить его на первом экране незачем.
            val session by auth.state.collectAsStateWithLifecycle()
            val pushEnabled by Push.enabled.collectAsStateWithLifecycle()
            val askNotifications = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) {}
            LaunchedEffect(session.isBootstrapping, session.user?.id, session.user?.academicGroup, pushEnabled) {
                Push.sync(this@MainActivity)
            }
            LaunchedEffect(session.user?.id) {
                if (session.user == null || !pushEnabled) return@LaunchedEffect
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Статус-бар всегда лежит на градиентной шапке — иконки там белые независимо
            // от темы. Навигационную полосу переключаем по выбранной теме, а не по системной:
            // штатный auto() смотрит на систему и на светлой теме поверх тёмной даёт
            // белые кнопки на белом.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(TRANSPARENT),
                    navigationBarStyle = if (darkTheme) SystemBarStyle.dark(TRANSPARENT)
                    else SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
                )
                onDispose {}
            }

            AppTheme(darkTheme = darkTheme) {
                if (entered) {
                    AppShell(
                        openWeek = pushWeek,
                        onWeekOpened = { pushWeek = null },
                        themeMode = themeMode,
                        onThemeChange = { mode ->
                            themeMode = mode
                            store.save(mode)
                        },
                    )
                } else {
                    AuthScreen(
                        onEnter = {
                            entryStore.markPassed()
                            entered = true
                        },
                    )
                }
            }
        }
    }
}
