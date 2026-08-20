package ru.dzhaparidze.mykct.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Индикатор загрузки — дуга, которая одновременно вращается и то вытягивается почти
 * в полный круг, то стягивается в точку.
 *
 * Порт веб-компонента на `stroke-dasharray`: там длина штриха идёт 1 → 400 → 800 при
 * длине окружности 2π·200 ≈ 1257, то есть дуга растёт от нуля до 229°, а `dashoffset`
 * тем временем уводит её начало. Здесь то же самое считается в градусах и рисуется
 * одним `drawArc` — растровой анимации и второй библиотеки для этого не нужно.
 *
 * Толщина штриха — доля диаметра (12.5%, как в оригинале), поэтому индикатор одинаково
 * выглядит и на 20dp в листе пары, и на 40dp в теле экрана.
 */
@Composable
fun Swirl(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    durationMillis: Int = 1500,
) {
    val transition = rememberInfiniteTransition(label = "swirl")
    // Дуга дышит: 0 → 1 и обратно (в вебе это `alternate`).
    val dash by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "swirl-dash",
    )
    // Оборот длиннее дыхания в 4/3 — периоды не совпадают, и цикл не «щёлкает».
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis * 4 / 3, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "swirl-spin",
    )

    Canvas(modifier = modifier.defaultMinSize(40.dp, 40.dp)) {
        val diameter = size.minDimension
        val stroke = diameter * 0.125f
        // Дуга рисуется по средней линии штриха, иначе половина толщины срезается краем.
        val box = Size(diameter - stroke, diameter - stroke)
        val topLeft = Offset((size.width - box.width) / 2f, (size.height - box.height) / 2f)
        // Смещение начала — ломаная (0 → -200 → -800 в вебе): вторая половина втрое быстрее.
        val start = if (dash < 0.5f) 114.6f * dash else 57.3f + 343.8f * (dash - 0.5f)
        drawArc(
            color = color,
            startAngle = spin + start,
            // 0.3° вместо нуля: у нулевой дуги круглые торцы не рисуются вовсе, и точка мигает.
            sweepAngle = 0.3f + dash * 229f,
            useCenter = false,
            topLeft = topLeft,
            size = box,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/** Что показывает экран прямо сейчас. Порядок важен: он же порядок проверок в [phaseOf]. */
enum class Phase { Loading, Error, Empty, Content }

fun phaseOf(isLoading: Boolean, error: String?, isEmpty: Boolean = false): Phase = when {
    isLoading -> Phase.Loading
    error != null -> Phase.Error
    isEmpty -> Phase.Empty
    else -> Phase.Content
}

/**
 * Плавная смена состояния экрана: индикатор гаснет, и на его месте проявляется контент
 * или ошибка. Подмена без перехода читается как рывок — особенно когда ответ пришёл
 * быстро и индикатор успел мелькнуть.
 *
 * Сначала уходит старое, потом появляется новое (задержка равна длительности ухода):
 * `AnimatedContent` держит оба кадра одновременно, и без разнесения по времени они
 * просвечивают друг сквозь друга. `SizeTransform(clip = false)` — из-за разной высоты
 * состояний: без него высокий контент подрезается по высоте индикатора.
 *
 * Содержимое кладётся в колонку, а не в `Box` от `AnimatedContent`: состояние экрана —
 * это почти всегда несколько элементов подряд, а в `Box` они лягут друг на друга.
 */
@Composable
fun <T> Fade(
    target: T,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(220, delayMillis = 90)) togetherWith
                fadeOut(tween(90)) using SizeTransform(clip = false)
        },
        label = "fade",
    ) { Column(modifier = Modifier.fillMaxWidth()) { content(it) } }
}
