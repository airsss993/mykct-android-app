package ru.dzhaparidze.mykct.ui

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.ui.theme.Violet
import ru.dzhaparidze.mykct.ui.theme.VioletDeep
import ru.dzhaparidze.mykct.ui.theme.VioletLight
import ru.dzhaparidze.mykct.ui.theme.VioletTint
import kotlin.math.hypot

/**
 * Главное действие экрана: пилюля во всю ширину, залитая акцентным градиентом
 * по диагонали (светлая лаванда — глубокий индиго), с бегущей по кромке
 * conic-подсветкой (референс shiny-cta).
 *
 * Sweep-градиент в Compose повернуть нечем, поэтому крутится не градиент, а холст:
 * `rotate` вокруг центра кнопки. Рисуем квадрат со стороной в диагональ —
 * прямоугольник по размеру кнопки при повороте открывал бы углы.
 *
 * Цвета не из палитры темы: кнопка одинаковая в светлой и тёмной, это её роль.
 */
@Composable
fun ShinyPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val angle by rememberInfiniteTransition(label = "shine").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "angle",
    )
    // Выключенная кнопка не светится: подсветка — приглашение нажать.
    val glow = if (enabled) 1f else 0f

    // Выключенная гасится целиком: на светлой заливке одного бледного текста мало.
    Box(modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f)) {
        if (CAN_BLUR && enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .blur(24.dp, BlurredEdgeTreatment.Unbounded)
                    .background(Violet.copy(alpha = 0.45f), CircleShape),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                // Подложка под кромкой: без неё в паузе между пробегами подсветки
                // сквозь 2dp рамки просвечивал фон экрана и кнопка казалась с зазором.
                .background(VioletDeep)
                .drawBehind {
                    val side = hypot(size.width, size.height)
                    rotate(angle) {
                        drawRect(
                            brush = Brush.sweepGradient(
                                0.00f to Color.Transparent,
                                0.06f to Violet.copy(alpha = glow),
                                0.12f to VioletTint.copy(alpha = glow),
                                0.18f to Violet.copy(alpha = glow),
                                0.24f to Color.Transparent,
                                1.00f to Color.Transparent,
                                center = center,
                            ),
                            topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f),
                            size = Size(side, side),
                        )
                    }
                }
                .padding(2.dp)
                .clip(CircleShape)
                // Светлая заливка: градиент по диагонали, от блика в левом верхнем углу
                // к глубокому индиго справа внизу.
                .background(
                    Brush.linearGradient(
                        0.00f to VioletTint,
                        0.35f to VioletLight,
                        1.00f to VioletDeep,
                    ),
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val content = Color.White
            if (icon != null) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = content,
                modifier = Modifier.padding(start = if (icon != null) 10.dp else 0.dp),
            )
        }
    }
}

/** Размытие свечения требует Android 12; ниже кнопка остаётся без ореола. */
private val CAN_BLUR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
