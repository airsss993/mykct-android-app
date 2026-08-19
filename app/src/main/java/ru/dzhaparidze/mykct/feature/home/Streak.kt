package ru.dzhaparidze.mykct.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.api.Attendance
import ru.dzhaparidze.mykct.data.api.AttendanceRecord
import ru.dzhaparidze.mykct.data.api.AttendanceStats
import ru.dzhaparidze.mykct.data.api.Streak
import ru.dzhaparidze.mykct.ui.hairline
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import ru.dzhaparidze.mykct.ui.theme.Violet
import ru.dzhaparidze.mykct.ui.theme.VioletDeep
import ru.dzhaparidze.mykct.ui.theme.VioletLight
import ru.dzhaparidze.mykct.ui.theme.VioletTint
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Огонёк переливается фирменными цветами: одна и та же линейная заливка ездит по
 * иконке, поэтому блик бежит снизу вверх. Отдельного оранжевого, как в референсе,
 * не заводим — второго акцентного цвета в палитре нет (см. DESIGN.md).
 */
private val FLAME = listOf(VioletTint, VioletLight, Violet, VioletDeep, Violet, VioletLight, VioletTint)

/** Живой огонёк: блик бежит по градиенту, свечение под ним дышит. */
@Composable
private fun Flame(diameter: Dp, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "streak")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    // Прозрачность анимируем цветом, а не alpha поверх размытия — см. DESIGN.md.
    val glow by infinite.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    // Свечение ещё и дышит в размере, но не выходит за половину бокса: за ней его
    // срежет граница компонента, и вместо ореола получится круг с обрубленным краем.
    val spread by infinite.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spread",
    )

    Box(
        modifier = modifier
            .size(diameter)
            .drawBehind {
                val radius = size.minDimension * spread
                drawCircle(
                    // Мягкий спад: на двух остановках у ореола видна кромка.
                    brush = Brush.radialGradient(
                        0f to VioletLight.copy(alpha = glow),
                        0.4f to VioletLight.copy(alpha = glow * 0.5f),
                        0.75f to VioletLight.copy(alpha = glow * 0.16f),
                        1f to Color.Transparent,
                        radius = radius,
                    ),
                    radius = radius,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_fire),
            contentDescription = null,
            modifier = Modifier
                .size(diameter * 0.5f)
                // SrcIn красит непрозрачные пиксели иконки градиентом, но только
                // в своём слое — без offscreen он затрёт всё, что нарисовано ниже.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = FLAME,
                            start = Offset(0f, size.height * (shift - 1f)),
                            end = Offset(0f, size.height * (shift + 0.6f)),
                        ),
                        blendMode = BlendMode.SrcIn,
                    )
                },
        )
    }
}

/** Кнопка-огонёк в углу шапки. Ряби нет намеренно: она спорит со свечением. */
@Composable
fun StreakFlame(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Flame(
        diameter = 44.dp,
        modifier = modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClickLabel = "Стрик посещений",
            onClick = onClick,
        ),
    )
}

/**
 * Что показывает огонёк: стрик, отметки за неделю и статус по посещаемости.
 * Данные те же, что на «Главной» ([HomeViewModel] один на приложение), поэтому
 * лист ничего не догружает — он открывается сразу заполненным.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakSheet(
    streak: Streak,
    records: List<AttendanceRecord>,
    stats: AttendanceStats,
    weekStart: LocalDate,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Flame(diameter = 132.dp)

            Text(
                text = streak.current.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = daysInRow(streak.current),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = status(streak.rate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(24.dp))
            WeekChecks(weekStart = weekStart, records = records)
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .hairline(RoundedCornerShape(20.dp))
                    .padding(vertical = 16.dp),
            ) {
                Stat("Дней", streak.daysAttended.toString(), Modifier.weight(1f))
                Stat("Пар", stats.total.toString(), Modifier.weight(1f))
                Stat("Посещал", "${streak.rate.toInt()}%", Modifier.weight(1f))
                Stat("Лучший", streak.longest.toString(), Modifier.weight(1f))
            }

            streak.periodStart?.let { start ->
                Text(
                    text = "С ${start.dayMonth()} · ${streak.daysAttended} из ${streak.schoolDays} учебных дней",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/**
 * Неделя как в референсе: день, на котором был, — кружок с галочкой, остальные —
 * просто число. Отметки берём из посещаемости той же недели, что открыта на «Главной».
 */
@Composable
private fun WeekChecks(weekStart: LocalDate, records: List<AttendanceRecord>) {
    val today = LocalDate.now()
    val byDate = records.groupBy { it.date }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (0..6).forEach { offset ->
            val date = weekStart.plusDays(offset.toLong())
            val attended = byDate[date].orEmpty().any { it.attendance == Attendance.PRESENT }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, RU_LOCALE).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .then(
                            if (attended) Modifier.background(AccentGradient, CircleShape) else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (attended) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_bold),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                            color = if (date > today) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val RU_LOCALE: Locale = Locale.forLanguageTag("ru-RU")

/** «5 дней подряд», «1 день подряд». Ноль — отдельный текст, «0 дней подряд» звучит зло. */
private fun daysInRow(count: Int): String =
    if (count == 0) "Стрик прервался" else "${days(count)} подряд"

private fun status(rate: Double): String = when {
    rate >= 90 -> "Ходишь почти без пропусков — так держать"
    rate >= 75 -> "Крепкая посещаемость, всё под контролем"
    rate >= 50 -> "Бывает по-разному — можно лучше"
    else -> "Пропусков много, пора возвращаться"
}
