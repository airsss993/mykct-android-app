package ru.dzhaparidze.mykct.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.ui.theme.AccentGradient

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

/**
 * Заголовок экрана — один на все три. Высота строки задана явно: в расписании рядом
 * с заголовком стоит пилюля группы (48dp), она растягивала строку, и текст там висел
 * ниже, чем на «Главной» и в настройках.
 */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .padding(top = 12.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * Круглая кнопка действия под сводкой экрана. Живёт здесь, а не в расписании:
 * тот же ряд стоит на «Главной», и разъехавшиеся размеры сразу читаются как
 * два разных экрана.
 */
@Composable
fun HeroAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = label,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Переключатель разделов внутри экрана: капсула с бегунком под активным пунктом,
 * залитым тем же `AccentGradient`, что и активный пункт навбара.
 *
 * Бегунок едет, а не перекрашивается на месте: без движения переключение читается
 * как перерисовка всего экрана, а не как переход между соседними разделами.
 * Ряби нет — под пальцем градиент, и она с ним спорит (см. DESIGN.md).
 */
@Composable
fun SegmentedSwitch(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // -1 — левый край, +1 — правый: `BiasAlignment` кладёт бегунок в долю ширины,
    // поэтому переключатель не зависит от числа пунктов и ширины экрана.
    val bias by animateFloatAsState(
        targetValue = if (items.size < 2) 0f else -1f + 2f * selected / (items.size - 1),
        animationSpec = tween(220),
        label = "segment",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .hairline(CircleShape)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f / items.size)
                .fillMaxHeight()
                .align(BiasAlignment(bias, 0f))
                .background(AccentGradient, CircleShape),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, title ->
                val color by animateColorAsState(
                    targetValue = if (index == selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "segment-label",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Обновление потягиванием вниз — единственный способ перезагрузить экран, отдельной
 * кнопки нет.
 *
 * Своё колесо вместо дефолтного нужно ровно ради `statusBarsPadding`: контейнер
 * растянут на весь экран, поэтому индикатор без отступа выезжает под часы и накрывает
 * строку заголовка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
            )
        },
        content = { content() },
    )
}
