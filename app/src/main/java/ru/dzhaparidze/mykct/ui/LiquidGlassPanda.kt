package ru.dzhaparidze.mykct.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import ru.dzhaparidze.mykct.R
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

// Кадр исходной картинки: все координаты ниже - в его пикселях.
private const val FRAME_W = 940f
private const val FRAME_H = 1672f
private val HEAD_AT = Offset(194f, 153f)
private val VISOR_AT = Offset(236f, 357f)
// Четыре строки кода на визоре: границы по серединам промежутков между ними.
private val VISOR_ROWS = floatArrayOf(357f, 417f, 445f, 473f, 533f)
private const val LOOP_MS = 6000

/**
 * Стеклянная панда экрана входа, оживлённая: ленты медленно плывут, по ним и по
 * голове пробегает блик, строки на визоре мерцают. Цикл 6 с, все движения - синусы
 * с целым числом периодов на цикл, поэтому шва нет.
 *
 * Слои разнесены заранее: ленты без панды дорисовал Codex (`imagegen`), голову и
 * визор вырезали маской из исходного кадра. Вписывается как `ContentScale.Crop`
 * с якорем сверху - так же, как стояла статичная картинка.
 *
 * При системной настройке "убрать анимации" (масштаб длительности 0) - статичный кадр.
 */
@Composable
fun LiquidGlassPanda(modifier: Modifier = Modifier) {
    val ribbons = ImageBitmap.imageResource(R.drawable.login_panda_ribbons)
    val head = ImageBitmap.imageResource(R.drawable.login_panda_head)
    val visor = ImageBitmap.imageResource(R.drawable.login_panda_visor)
    val resolver = LocalContext.current.contentResolver
    val still = remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val time: State<Float> = if (still) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "panda").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing)),
            label = "t",
        )
    }
    val layer = remember { Paint().apply { blendMode = BlendMode.Plus } }

    // Время читается только здесь, в фазе отрисовки: кадр перерисовывается без рекомпозиции.
    Canvas(modifier) {
        val s = max(size.width / FRAME_W, size.height / FRAME_H)
        translate(left = (size.width - FRAME_W * s) / 2) {
            scale(s, pivot = Offset.Zero) {
                drawPanda(time.value, ribbons, head, visor, layer)
            }
        }
    }
}

private fun DrawScope.drawPanda(t: Float, ribbons: ImageBitmap, head: ImageBitmap, visor: ImageBitmap, layer: Paint) {
    val a = t * 2 * PI.toFloat()
    val frame = Rect(0f, 0f, FRAME_W, FRAME_H)

    // Ленты: дрейф по эллипсу, покачивание и дыхание масштаба. Запас 6% по краям
    // не даёт открыться краю картинки.
    val flow: DrawScope.() -> Unit = {
        withTransform({
            translate(sin(a) * 16f, sin(a + PI.toFloat() / 2) * 10f)
            rotate(sin(a) * 1.2f, pivot = Offset(470f, 500f))
            scale(1.06f + 0.02f * sin(2 * a), pivot = Offset(470f, 600f))
        }) { drawImage(ribbons) }
    }
    flow()
    // Свет бежит по лентам диагональю сверху вниз; на краях цикла полоса за кадром.
    glint(frame, layer, Offset(0.6f, 0.8f), -400f + 2500f * t, 320f, 0.5f) { flow() }

    // Голова чуть покачивается, визор едет вместе с ней.
    translate(top = sin(a) * 4f) {
        drawImage(head, HEAD_AT)
        // Блик по стеклу головы проходит в первой половине цикла, в остальное время слоя нет.
        if (t in 0.1f..0.6f) {
            val bounds = Rect(HEAD_AT, Size(head.width.toFloat(), head.height.toFloat()))
            glint(bounds, layer, Offset(0.94f, 0.34f), -200f + 1400f * (t - 0.1f) / 0.5f, 200f, 0.55f) {
                drawImage(head, HEAD_AT)
            }
        }
        for (i in 0 until VISOR_ROWS.size - 1) {
            val w = 0.5f + 0.5f * sin(a * (2 + i) + i * 1.9f)
            clipRect(top = VISOR_ROWS[i], bottom = VISOR_ROWS[i + 1]) {
                drawImage(visor, VISOR_AT, alpha = 0.1f + 0.6f * w * w * w, blendMode = BlendMode.Plus)
            }
        }
    }
}

/**
 * Прибавляет к кадру [content], видимый только под светлой полосой ширины [width]
 * с центром в [center] вдоль направления [dir]. Свой слой нужен, чтобы маска DstIn
 * резала только [content], а не всё, что нарисовано раньше.
 */
private inline fun DrawScope.glint(
    bounds: Rect,
    layer: Paint,
    dir: Offset,
    center: Float,
    width: Float,
    strength: Float,
    content: DrawScope.() -> Unit,
) {
    drawContext.canvas.saveLayer(bounds, layer)
    content()
    drawRect(
        Brush.linearGradient(
            0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = strength),
            1f to Color.Transparent,
            start = dir * (center - width / 2),
            end = dir * (center + width / 2),
        ),
        blendMode = BlendMode.DstIn,
    )
    drawContext.canvas.restore()
}
