package ru.dzhaparidze.mykct.feature

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.ThemeMode
import ru.dzhaparidze.mykct.feature.auth.LoginScreen
import ru.dzhaparidze.mykct.feature.home.HomeScreen
import ru.dzhaparidze.mykct.feature.schedule.ScheduleScreen
import ru.dzhaparidze.mykct.ui.dotGrid
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import ru.dzhaparidze.mykct.feature.settings.SettingsScreen

enum class Screen { SCHEDULE, HOME, SETTINGS }

/**
 * Высота, которую навбар отъедает у контента снизу: экраны докладывают её сами.
 * Сама капсула плюс системная навигация под ней — без неё жест-бар съедал нижние
 * 24–48 dp контента, и последний элемент экрана оказывался под навбаром.
 */
@Composable
fun navBarInset(): Dp = 108.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Оболочка приложения: экран + плавающий навбар поверх него.
 * Навигации как таковой нет — три экрана и переключатель, роутер тут не за что.
 */
@Composable
fun AppShell(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    // Старт на расписании: «Главная» пока пустая, открывать приложение на заглушке незачем.
    var screen by rememberSaveable { mutableStateOf(Screen.SCHEDULE) }
    // Форма входа живёт здесь, а не на экранах: навбар плавает поверх контента,
    // и открытая изнутри экрана форма оказывалась под ним.
    var loginOpen by rememberSaveable { mutableStateOf(false) }

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
        // Экраны сменяются наплывом: сначала старый гаснет, потом проявляется новый.
        // Одновременный `Crossfade` не годится — в середине оба кадра полупрозрачны,
        // текст ложится на текст, а сквозь них просвечивает подложка окна.
        // Заливка фоном обязательна по той же причине: между уходом и появлением
        // на весь экран видно окно, и без неё там мелькает белая пелена.
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                fadeIn(tween(200, delayMillis = 120)) togetherWith fadeOut(tween(120))
            },
            label = "screen",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) { current ->
            when (current) {
                Screen.SCHEDULE -> ScheduleScreen()
                Screen.HOME -> HomeScreen(onLogin = { loginOpen = true })

                Screen.SETTINGS -> SettingsScreen(
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    onLogin = { loginOpen = true },
                )
            }
        }
        }

        NavBar(
            backdrop = backdrop,
            current = screen,
            onSelect = { screen = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (loginOpen) {
            LoginScreen(onBack = { loginOpen = false }, onSuccess = { loginOpen = false })
        }
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
            // Прозрачность зашита в цвет, а не в graphicsLayer: отдельный слой поверх
            // размытия уводит его на другой путь отрисовки, и на время анимации
            // вместо круглого ореола видно размытый квадрат по границам слоя.
            Box(
                modifier = Modifier
                    .size(NAV_ITEM_SIZE)
                    .blur(14.dp, BlurredEdgeTreatment.Unbounded)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = selected),
                        CircleShape,
                    ),
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
