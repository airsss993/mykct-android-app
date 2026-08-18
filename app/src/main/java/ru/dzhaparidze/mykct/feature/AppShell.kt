package ru.dzhaparidze.mykct.feature

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.ThemeMode
import ru.dzhaparidze.mykct.feature.schedule.ScheduleScreen
import ru.dzhaparidze.mykct.ui.dotGrid
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

    // Подложка под навбар: контент экрана пишется в слой, навбар рисует его размытым
    // у себя под пластиной. Настоящего backdrop-blur в Compose нет, это его ручная сборка.
    val backdrop = rememberGraphicsLayer()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)
                },
        ) {
        when (screen) {
            Screen.SCHEDULE -> ScheduleScreen()
            Screen.HOME -> ComingSoon(
                title = "Главная",
                text = "Здесь появятся успеваемость и посещаемость.\nЖдём авторизацию — без неё бэкенд их не отдаёт.",
            )

            Screen.SETTINGS -> SettingsScreen(themeMode = themeMode, onThemeChange = onThemeChange)
        }
        }

        NavBar(
            backdrop = backdrop,
            current = screen,
            onSelect = { screen = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Нижняя панель как в референсе: полупрозрачная пластина во всю ширину, прижата
 * к низу экрана, скруглены только верхние углы — контент просвечивает сквозь неё.
 * У активного пункта — светлый круг, приподнятый над верхней кромкой; поэтому
 * строка пунктов лежит поверх Surface, а не внутри него: Surface режет содержимое
 * по своей форме и круг бы обрезался.
 */
@Composable
private fun NavBar(
    backdrop: GraphicsLayer,
    current: Screen,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Куда сдвинуть записанный слой, чтобы под пластиной оказался тот же кусок экрана.
    var barTop by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { barTop = it.positionInRoot().y },
    ) {
        if (CAN_BLUR) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(BAR_SHAPE)
                    .graphicsLayer { renderEffect = BlurEffect(BLUR_RADIUS.toPx(), BLUR_RADIUS.toPx()) }
                    .drawBehind { translate(top = -barTop) { drawLayer(backdrop) } },
            )
        }

        Surface(
            shape = BAR_SHAPE,
            // Поверх размытия — тонкая плёнка цвета панели; без размытия (Android 11 и
            // ниже RenderEffect не умеет) она почти непрозрачная, иначе текст просвечивает.
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (CAN_BLUR) 0.62f else 0.94f),
            // Тень под полупрозрачной пластиной просвечивает насквозь и мутит её,
            // поэтому вместо неё — светлая волосяная кромка сверху, как в референсе.
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
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
                icon = R.drawable.ic_calendar,
                label = "Расписание",
                isSelected = current == Screen.SCHEDULE,
                onClick = { onSelect(Screen.SCHEDULE) },
                modifier = Modifier.weight(1f),
            )
            NavItem(
                icon = R.drawable.ic_home,
                label = "Главная",
                isSelected = current == Screen.HOME,
                onClick = { onSelect(Screen.HOME) },
                modifier = Modifier.weight(1f),
            )
            NavItem(
                icon = R.drawable.ic_settings,
                label = "Настройки",
                isSelected = current == Screen.SETTINGS,
                onClick = { onSelect(Screen.SETTINGS) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val BAR_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val BLUR_RADIUS = 24.dp

/** RenderEffect появился в Android 12; ниже размытия нет и пластина просто плотнее. */
private val CAN_BLUR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val NAV_BAR_HEIGHT = 66.dp

@Composable
private fun NavItem(
    @DrawableRes icon: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Пункт из референса: у активного — пилюля с иконкой и подписью в строку,
    // у остальных только иконка. Подпись появляется вместе с пилюлей, поэтому
    // одна анимируемая величина на всё: и заливка, и цвет, и раскрытие.
    val selected by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(220),
        label = "nav-selected",
    )
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val content = lerp(idle, accent, selected)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                // рябь по всей ячейке спорит с пилюлей, поэтому её нет
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f * selected))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = if (isSelected) null else label,
                tint = content,
                modifier = Modifier.size(24.dp),
            )
            if (selected > 0f) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        // ширина едет вместе с прозрачностью — подпись не прыгает целиком
                        .graphicsLayer { alpha = selected }
                        .widthIn(max = 200.dp * selected),
                )
            }
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
            .dotGrid()
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
