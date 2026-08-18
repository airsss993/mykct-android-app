package ru.dzhaparidze.mykct.ui

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Палитра из веб-версии кнопки (те же hsl-цвета, порядок остановок тоже её).
private val RAINBOW = listOf(
    Color(0xFFFF4242), // --color-1  hsl(0 100% 63%)
    Color(0xFF85FF42), // --color-5  hsl(90 100% 63%)
    Color(0xFF4295FF), // --color-3  hsl(210 100% 63%)
    Color(0xFF42D6FF), // --color-4  hsl(195 100% 63%)
    Color(0xFFA342FF), // --color-2  hsl(270 100% 63%)
    Color(0xFFFF4242), // замыкаем на первый, иначе на стыке тайлов видна граница
)

private const val CYCLE_MS = 2000
private val BORDER = 2.dp

// Тёмная тема веб-версии: тело кнопки белое, текст — тёмный.
// (Светлый вариант там наоборот: тело #121213, текст белый.)
private val INNER = Color.White
private val LABEL = Color(0xFF121213)

/**
 * Кнопка с бегущей радугой: радужная рамка, тёмная заливка и размытое свечение снизу.
 * Порт веб-компонента rainbow-button — там это `background-clip: padding-box, border-box`
 * плюс `filter: blur()`; здесь рамка рисуется двумя скруглёнными прямоугольниками,
 * а свечение — отдельным размытым слоем под кнопкой.
 *
 * Размытие свечения требует Android 12 (`Modifier.blur` ниже — no-op), там остаётся
 * чистая радужная рамка.
 */
@Composable
fun RainbowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
) {
    val transition = rememberInfiniteTransition(label = "rainbow")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "rainbow-shift",
    )

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (CAN_BLUR) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(height / 4)
                    .offset(y = height / 2)
                    // Unbounded, иначе размытие обрезается по краям слоя и свечение
                    // выглядит цветной полосой с резкими кромками.
                    .blur(14.dp, BlurredEdgeTreatment.Unbounded)
                    .drawBehind { drawRect(rainbow(size.width, shift)) },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .drawBehind {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(brush = rainbow(size.width, shift), cornerRadius = radius)

                    val border = BORDER.toPx()
                    drawRoundRect(
                        color = INNER,
                        topLeft = Offset(border, border),
                        size = Size(size.width - border * 2, size.height - border * 2),
                        cornerRadius = CornerRadius(radius.x - border),
                    )
                }
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = LABEL,
            )
        }
    }
}

/** Градиент шириной в кнопку, сдвинутый на [shift] цикла и замощённый по горизонтали. */
private fun rainbow(width: Float, shift: Float): Brush = Brush.linearGradient(
    colors = RAINBOW,
    start = Offset(-shift * width, 0f),
    end = Offset(width - shift * width, 0f),
    tileMode = TileMode.Repeated,
)

private val CAN_BLUR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
