package ru.dzhaparidze.mykct.ui

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Точечная сетка на фоне — фактура вместо плоской заливки, как в референсе нодового
 * редактора.
 *
 * Список точек считается в `drawWithCache` (пересчёт только при смене размера) и
 * рисуется одним `drawPoints`. Наивная версия — цикл с `drawCircle` в `drawBehind` —
 * давала ~800 вызовов на кадр на всю высоту прокручиваемого листа и вешала экран
 * до ANR при скролле. Проверено: 78–118% CPU против единиц процентов сейчас.
 */
@Composable
fun Modifier.dotGrid(step: Dp = 24.dp, dot: Dp = 1.dp): Modifier {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    return drawWithCache {
        val gap = step.toPx()
        // width/height берём заранее: внутри buildList `size` — это размер списка
        val w = size.width
        val h = size.height
        val points = buildList {
            var y = gap / 2f
            while (y < h) {
                var x = gap / 2f
                while (x < w) {
                    add(Offset(x, y))
                    x += gap
                }
                y += gap
            }
        }
        onDrawBehind {
            drawPoints(
                points = points,
                pointMode = PointMode.Points,
                color = color,
                strokeWidth = dot.toPx() * 2,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Волосяная светлая кромка карточки — она отделяет её от фона без тени. */
@Composable
fun Modifier.hairline(shape: Shape): Modifier =
    border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
