package ru.dzhaparidze.mykct.ui

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Точечная сетка на фоне — фактура вместо плоской заливки, как в референсе нодового
 * редактора. Точки почти не видно поимённо, но плоскость перестаёт быть пустой.
 *
 * ponytail: точки пересобираются каждый кадр (≈700 штук на экран). На эмуляторе и
 * реальном скролле не заметно; если начнёт лагать — печь тайл в ImageBitmap и
 * заливать `ShaderBrush` с TileMode.Repeated.
 */
@Composable
fun Modifier.dotGrid(step: Dp = 24.dp, dot: Dp = 1.dp): Modifier {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    return drawBehind {
        val gap = step.toPx()
        val radius = dot.toPx()
        var y = gap / 2f
        while (y < size.height) {
            var x = gap / 2f
            while (x < size.width) {
                drawCircle(color = color, radius = radius, center = Offset(x, y))
                x += gap
            }
            y += gap
        }
    }
}

/** Волосяная светлая кромка карточки — она отделяет её от фона без тени. */
@Composable
fun Modifier.hairline(shape: Shape): Modifier =
    border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
