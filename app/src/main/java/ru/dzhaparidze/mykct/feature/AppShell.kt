package ru.dzhaparidze.mykct.feature

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.data.ThemeMode
import ru.dzhaparidze.mykct.feature.schedule.ScheduleScreen
import ru.dzhaparidze.mykct.feature.settings.SettingsScreen

enum class Screen { SCHEDULE, HOME, SETTINGS }

/** Высота, которую навбар отъедает у контента снизу: экраны докладывают её сами. */
val NAV_BAR_INSET = 112.dp

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
 * Нижняя панель как в референсе: пластина во всю ширину, прижата к низу экрана,
 * скруглены только верхние углы. У активного пункта — светлый круг, приподнятый
 * над верхней кромкой панели; поэтому строка пунктов лежит поверх Surface, а не
 * внутри него: Surface режет содержимое по своей форме и круг бы обрезался.
 */
@Composable
private fun NavBar(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            content = {},
            modifier = Modifier.matchParentSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(NAV_BAR_HEIGHT),
        ) {
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

private val NAV_BAR_HEIGHT = 74.dp
private val NAV_PILL_SIZE = 44.dp

/** На сколько круг активного пункта выступает над кромкой панели. */
private val NAV_PILL_RAISE = (-16).dp

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Одна анимируемая величина на весь пункт: круг проявляется, иконка и подпись
    // одновременно доезжают до белого — иначе они расходятся по времени и это видно.
    val selected by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(220),
        label = "nav-selected",
    )
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    // Круг светлый, иконка внутри — цвета панели: в референсе акцента в навбаре нет,
    // активный пункт выделен контрастом, а не цветом.
    val pill = MaterialTheme.colorScheme.onSurface
    val iconTint = lerp(idle, MaterialTheme.colorScheme.surface, selected)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                // рябь по всей ячейке спорит с приподнятым кругом, поэтому её нет
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(NAV_PILL_SIZE)
                // offset, а не padding: подъём не должен менять раскладку соседей
                .offset(y = NAV_PILL_RAISE * selected),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(selected)
                    .background(pill, CircleShape),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = lerp(idle, MaterialTheme.colorScheme.onSurface, selected),
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
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
