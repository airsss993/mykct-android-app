package ru.dzhaparidze.mykct

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.dzhaparidze.mykct.data.ThemeMode
import ru.dzhaparidze.mykct.data.ThemeStore
import ru.dzhaparidze.mykct.feature.AppShell
import ru.dzhaparidze.mykct.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { ThemeStore(this) }
            var themeMode by remember { mutableStateOf(store.load()) }
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
                AppShell(
                    themeMode = themeMode,
                    onThemeChange = { mode ->
                        themeMode = mode
                        store.save(mode)
                    },
                )
            }
        }
    }
}
