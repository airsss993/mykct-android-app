package ru.dzhaparidze.mykct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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

            AppTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
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
