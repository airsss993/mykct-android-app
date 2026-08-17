package ru.dzhaparidze.mykct.feature

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dzhaparidze.mykct.data.ThemeMode
import ru.dzhaparidze.mykct.feature.schedule.ScheduleScreen
import ru.dzhaparidze.mykct.feature.settings.SettingsScreen

enum class Screen { SCHEDULE, HOME, SETTINGS }

/** Высота, которую навбар отъедает у контента снизу: экраны докладывают её сами. */
val NAV_BAR_INSET = 104.dp

/**
 * Оболочка приложения: экран + плавающий навбар поверх него.
 * Навигации как таковой нет — три экрана и переключатель, роутер тут не за что.
 */
@Composable
fun AppShell(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    // Старт на расписании: «Главная» пока пустая, открывать приложение на заглушке незачем.
    var screen by rememberSaveable { mutableStateOf(Screen.SCHEDULE) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.SCHEDULE -> ScheduleScreen()
            Screen.HOME -> ComingSoon(
                title = "Главная",
                text = "Здесь появятся успеваемость и посещаемость.\nЖдём авторизацию — без неё бэкенд их не отдаёт.",
            )

            Screen.SETTINGS -> SettingsScreen(themeMode = themeMode, onThemeChange = onThemeChange)
        }

        NavBar(
            current = screen,
            onSelect = { screen = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Плавающая пилюля из референса: по бокам обычные пункты, по центру — крупная
 * акцентная кнопка главной.
 */
@Composable
private fun NavBar(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NavItem(
                    icon = Icons.Default.DateRange,
                    label = "Расписание",
                    isSelected = current == Screen.SCHEDULE,
                    onClick = { onSelect(Screen.SCHEDULE) },
                )

                HomeItem(
                    isSelected = current == Screen.HOME,
                    onClick = { onSelect(Screen.HOME) },
                )

                NavItem(
                    icon = Icons.Default.Settings,
                    label = "Настройки",
                    isSelected = current == Screen.SETTINGS,
                    onClick = { onSelect(Screen.SETTINGS) },
                )
            }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)
        },
        animationSpec = tween(250),
        label = "nav-item-bg",
    )
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Центральная кнопка: всегда акцентная, выбранное состояние показывает кольцо. */
@Composable
private fun HomeItem(isSelected: Boolean, onClick: () -> Unit) {
    val size by animateDpAsState(
        targetValue = if (isSelected) 58.dp else 52.dp,
        animationSpec = tween(250),
        label = "home-size",
    )

    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Главная",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** Экран, которого пока нет: честно пишем, чего ждать, вместо пустоты. */
@Composable
private fun ComingSoon(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
