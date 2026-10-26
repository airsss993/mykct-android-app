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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
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
private val GOGGLES_AT = Offset(211f, 357f)
private val CODE_AT = Offset(236f, 357f)
// Зрачки в глазах под визором.
private val PUPILS = arrayOf(Offset(359f, 438f), Offset(574f, 438f))
// Строки кода на визоре: верх и низ полосы клипа, где строка начинается и кончается.
private val CODE_ROWS = arrayOf(
    floatArrayOf(357f, 417f, 268f, 397f),
    floatArrayOf(417f, 445f, 294f, 444f),
    floatArrayOf(445f, 473f, 293f, 389f),
    floatArrayOf(473f, 533f, 250f, 344f),
)
private val CODE_COLOR = Color(0xFFDC30FB)
private val PUPIL_GLOW = Color(0xFFE9D8FF)
private const val LOOP_MS = 8000
// Без анимаций показываем момент, когда весь код уже набран и визор опущен.
private const val STILL_T = 0.5f
// Сетка, которой гнутся ленты: клетка около 67x70 пикселей кадра.
private const val MESH_W = 14
private const val MESH_H = 24

/**
 * Стеклянная панда экрана входа, оживлённая. Цикл 8 с:
 * - ленты всё время текут волной, каждая полоса со своей фазой, по ним бежит свет;
 * - 0-0.4 на визоре строка за строкой печатается код с курсором, 0.4-0.6 код горит,
 *   по стеклу проходит блик, потом код гаснет;
 * - 0.62-0.7 панда поднимает визор на лоб, под ним глаза: оглядываются, моргают;
 * - 0.88-0.97 визор падает обратно с отскоком, экран пустой до нового цикла.
 *
 * Все движения периодичны по циклу, поэтому шва нет.
 *
 * Слои: ленты без панды и морду с глазами без визора дорисовал Codex (`imagegen`,
 * правка только полосы визора, остальное совпадает с исходником пиксель в пиксель),
 * визор вырезан маской из исходного кадра, строки кода с него вычищены и лежат
 * отдельным слоем со свечением. Вписывается как `ContentScale.Crop` с якорем
 * сверху - так же, как стояла статичная картинка.
 *
 * При системной настройке "убрать анимации" (масштаб длительности 0) - статичный кадр.
 */
@Composable
fun LiquidGlassPanda(modifier: Modifier = Modifier) {
    val ribbons = ImageBitmap.imageResource(R.drawable.login_panda_ribbons)
    val head = ImageBitmap.imageResource(R.drawable.login_panda_head)
    val goggles = ImageBitmap.imageResource(R.drawable.login_panda_goggles)
    val code = ImageBitmap.imageResource(R.drawable.login_panda_visor)
    val resolver = LocalContext.current.contentResolver
    val still = remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val time: State<Float> = if (still) {
        remember { mutableFloatStateOf(STILL_T) }
    } else {
        rememberInfiniteTransition(label = "panda").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing)),
            label = "t",
        )
    }
    val layer = remember { Paint().apply { blendMode = BlendMode.Plus } }
    val mesh = remember { Mesh(ribbons.asAndroidBitmap()) }

    // Время читается только здесь, в фазе отрисовки: кадр перерисовывается без рекомпозиции.
    Canvas(modifier) {
        val s = max(size.width / FRAME_W, size.height / FRAME_H)
        translate(left = (size.width - FRAME_W * s) / 2) {
            scale(s, pivot = Offset.Zero) {
                drawPanda(time.value, mesh, head, goggles, code, layer)
            }
        }
    }
}

/** Ленты, натянутые на сетку: вершины сдвигаются волной, картинка гнётся вслед. */
private class Mesh(val bitmap: android.graphics.Bitmap) {
    val verts = FloatArray((MESH_W + 1) * (MESH_H + 1) * 2)
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

    fun draw(canvas: android.graphics.Canvas, a: Float) {
        var k = 0
        for (j in 0..MESH_H) {
            val y = FRAME_H * j / MESH_H
            // У каждой полосы по высоте своя амплитуда: соседние ленты гнутся вразнобой.
            val amp = 34f * (0.55f + 0.45f * sin(y / 140f))
            for (i in 0..MESH_W) {
                val x = FRAME_W * i / MESH_W
                verts[k++] = x + 14f * sin(y / 210f + 2 * a + x / 400f)
                // Поперечная волна бежит вдоль лент слева направо, один пробег за цикл.
                verts[k++] = y + amp * sin(x / 160f - a + y / 260f)
            }
        }
        canvas.drawBitmapMesh(bitmap, MESH_W, MESH_H, verts, 0, null, 0, paint)
    }
}

private fun DrawScope.drawPanda(
    t: Float,
    mesh: Mesh,
    head: ImageBitmap,
    goggles: ImageBitmap,
    code: ImageBitmap,
    layer: Paint,
) {
    val a = t * 2 * PI.toFloat()
    val frame = Rect(0f, 0f, FRAME_W, FRAME_H)

    // Запас масштаба 6% прячет края сетки, которые волна отводит от краёв кадра.
    val flow: DrawScope.() -> Unit = {
        scale(1.06f, pivot = Offset(FRAME_W / 2, FRAME_H / 2)) {
            mesh.draw(drawContext.canvas.nativeCanvas, a)
        }
    }
    flow()
    // Свет бежит по лентам диагональю сверху вниз; на краях цикла полоса за кадром.
    glint(frame, layer, Offset(0.6f, 0.8f), -400f + 2500f * t, 320f, 0.5f) { flow() }

    // Голова чуть покачивается, всё остальное едет вместе с ней.
    translate(top = sin(a) * 4f) {
        drawImage(head, HEAD_AT)
        val lift = goggleLift(t)
        if (lift > 0f) drawEyes(t)
        // Поднятый визор уезжает на лоб и сплющивается по высоте: наклонился к зрителю верхом.
        withTransform({
            translate(top = -110f * lift)
            scale(1f, 1f - 0.3f * lift, pivot = GOGGLES_AT)
        }) {
            drawImage(goggles, GOGGLES_AT)
            drawCode(t, code)
        }
        // Блик по стеклу, когда код набран.
        if (t in 0.4f..0.6f) {
            val bounds = Rect(HEAD_AT, Size(head.width.toFloat(), head.height.toFloat()))
            glint(bounds, layer, Offset(0.94f, 0.34f), -200f + 1400f * (t - 0.4f) / 0.2f, 200f, 0.55f) {
                drawImage(head, HEAD_AT)
                drawImage(goggles, GOGGLES_AT)
            }
        }
    }
}

/** Насколько поднят визор: 0 - на глазах, 1 - на лбу. Опускается с отскоком. */
private fun goggleLift(t: Float): Float = when {
    t < 0.62f -> 0f
    t < 0.7f -> easeOut((t - 0.62f) / 0.08f)
    t < 0.88f -> 1f
    t < 0.94f -> 1f - easeIn((t - 0.88f) / 0.06f)
    t < 0.97f -> 0.06f * sin((t - 0.94f) / 0.03f * PI.toFloat())
    else -> 0f
}

private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x)
private fun easeIn(x: Float) = x * x

/**
 * Глаза под визором. Зашитые в картинку зрачки закрыты чёрным, поверх свои:
 * светящиеся точки, которые смотрят влево, вправо и моргают перед тем, как визор упадёт.
 */
private fun DrawScope.drawEyes(t: Float) {
    val look = if (t in 0.7f..0.86f) 14f * sin((t - 0.7f) / 0.16f * 2 * PI.toFloat()) else 0f
    val open = if (t in 0.835f..0.855f) 0f else 1f
    for (p in PUPILS) {
        drawCircle(Brush.radialGradient(0.6f to Color.Black, 1f to Color.Transparent, center = p, radius = 30f), 30f, p)
        val c = p + Offset(look, 0f)
        drawCircle(
            Brush.radialGradient(0f to PUPIL_GLOW, 0.35f to PUPIL_GLOW.copy(alpha = 0.5f), 1f to Color.Transparent, center = c, radius = 26f),
            26f,
            c,
            alpha = open,
            blendMode = BlendMode.Plus,
        )
        drawCircle(Color.White, 8f, c, alpha = open)
    }
}

/**
 * Код на визоре: 0-0.4 строки печатаются по очереди, за краем набранного мигает
 * курсор; 0.4-0.55 код горит и пульсирует; 0.55-0.61 мигает и гаснет.
 */
private fun DrawScope.drawCode(t: Float, code: ImageBitmap) {
    val typing = 0.4f
    val perRow = typing / CODE_ROWS.size
    val alpha = when {
        t < 0.55f -> 0.85f + 0.15f * sin(t * 40f)
        t < 0.61f -> if (((t - 0.55f) * 80f).toInt() % 2 == 0) 0.9f else 0.2f
        else -> 0f
    }
    if (alpha == 0f) return
    val blink = ((t * 16f).toInt() % 2 == 0)
    CODE_ROWS.forEachIndexed { i, (top, bottom, from, to) ->
        val p = ((t - i * perRow) / perRow).coerceIn(0f, 1f)
        if (p == 0f) return@forEachIndexed
        val edge = from + (to - from) * p
        clipRect(right = edge, top = top, bottom = bottom) {
            drawImage(code, CODE_AT, alpha = alpha)
        }
        val current = p < 1f || (i == CODE_ROWS.lastIndex && t < 0.55f)
        if (current && blink) {
            val mid = (top + bottom) / 2
            drawRect(CODE_COLOR, Offset(edge + 4f, mid - 9f), Size(12f, 18f), alpha = alpha)
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
