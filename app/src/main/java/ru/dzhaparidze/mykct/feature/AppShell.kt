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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
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
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import ru.dzhaparidze.mykct.feature.settings.SettingsScreen

enum class Screen { SCHEDULE, HOME, SETTINGS }

/** Высота, которую навбар отъедает у контента снизу: экраны докладывают её сами. */
val NAV_BAR_INSET = 108.dp

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
    var barLeft by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 20.dp)
            .onGloballyPositioned {
                barTop = it.positionInRoot().y
                barLeft = it.positionInRoot().x
            },
    ) {
        if (CAN_BLUR) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .graphicsLayer { renderEffect = BlurEffect(BLUR_RADIUS.toPx(), BLUR_RADIUS.toPx()) }
                    .drawBehind { translate(left = -barLeft, top = -barTop) { drawLayer(backdrop) } },
            )
        }

        Surface(
            shape = CircleShape,
            // Поверх размытия — плёнка цвета панели; без размытия (Android 11 и ниже
            // RenderEffect не умеет) она почти непрозрачная, иначе текст просвечивает.
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (CAN_BLUR) 0.72f else 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
            content = {},
            modifier = Modifier.matchParentSize(),
        )

        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NavItem(
                icon = R.drawable.ic_calendar,
                label = "Расписание",
                isSelected = current == Screen.SCHEDULE,
                onClick = { onSelect(Screen.SCHEDULE) },
            )
            NavItem(
                icon = R.drawable.ic_home,
                label = "Главная",
                isSelected = current == Screen.HOME,
                onClick = { onSelect(Screen.HOME) },
            )
            NavItem(
                icon = R.drawable.ic_settings,
                label = "Настройки",
                isSelected = current == Screen.SETTINGS,
                onClick = { onSelect(Screen.SETTINGS) },
            )
        }
    }
}

private val BLUR_RADIUS = 24.dp
private val NAV_ITEM_SIZE = 56.dp

/** RenderEffect появился в Android 12; ниже размытия нет и пластина просто плотнее. */
private val CAN_BLUR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Пункт-кружок из референса: активный залит фирменным градиентом и светится,
 * остальные — тёмные кружки. Подписей нет, они у капсулы не помещаются;
 * название остаётся в contentDescription для TalkBack.
 */
@Composable
private fun NavItem(
    @DrawableRes icon: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val selected by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(220),
        label = "nav-selected",
    )
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    val content = lerp(idle, Color.White, selected)

    Box(contentAlignment = Alignment.Center) {
        if (CAN_BLUR && selected > 0f) {
            // Свечение под активным кружком — тот же приём, что у идущей пары.
            Box(
                modifier = Modifier
                    .size(NAV_ITEM_SIZE)
                    .graphicsLayer { alpha = selected }
                    .blur(14.dp, BlurredEdgeTreatment.Unbounded)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .size(NAV_ITEM_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .selectable(
                    selected = isSelected,
                    role = Role.Tab,
                    // рябь спорит с градиентом и свечением, поэтому её нет
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(selected)
                    .background(AccentGradient, CircleShape),
            )
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = content,
                modifier = Modifier.size(24.dp),
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
