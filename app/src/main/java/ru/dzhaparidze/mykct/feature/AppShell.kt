package ru.dzhaparidze.mykct.feature

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
 * Плавающая панель из референса: широкая скруглённая пластина, тонкие иконки без
 * подписей, у активного пункта — зелёное свечение от верхней кромки.
 */
@Composable
private fun NavBar(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.height(NAV_BAR_HEIGHT)) {
                NavItem(
                    icon = Icons.Outlined.DateRange,
                    label = "Расписание",
                    isSelected = current == Screen.SCHEDULE,
                    onClick = { onSelect(Screen.SCHEDULE) },
                    modifier = Modifier.weight(1f),
                )
                NavItem(
                    icon = Icons.Outlined.Home,
                    label = "Главная",
                    isSelected = current == Screen.HOME,
                    onClick = { onSelect(Screen.HOME) },
                    modifier = Modifier.weight(1f),
                )
                NavItem(
                    icon = Icons.Outlined.Settings,
                    label = "Настройки",
                    isSelected = current == Screen.SETTINGS,
                    onClick = { onSelect(Screen.SETTINGS) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val NAV_BAR_HEIGHT = 64.dp

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.secondary
    val tint by animateColorAsState(
        targetValue = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "nav-tint",
    )
    val glow by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(250),
        label = "nav-glow",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                // рябь по всей ячейке спорит со свечением, поэтому её нет
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            // Свечение бьёт из верхней кромки панели: радиальное, иначе получается
            // прямоугольная плашка во всю ячейку вместо мягкого пятна.
            .drawBehind {
                if (glow > 0f) {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.5f * glow), Color.Transparent),
                            center = Offset(size.width / 2f, 0f),
                            // радиус меньше полуширины ячейки, иначе пятно обрывается
                            // об её край вертикальной линией вместо мягкого затухания
                            radius = minOf(size.width / 2f, size.height) * 0.85f,
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
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
