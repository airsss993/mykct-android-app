package ru.dzhaparidze.mykct.feature.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.feature.schedule.DayCell
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val RU = Locale.forLanguageTag("ru-RU")

/**
 * Полоса недели из референса: подпись дня, круг с числом, точки = количество пар.
 * В референсе полоса скроллится, но у нас неделя фиксированная (пн–вс) и целиком
 * влезает в ширину — круги делят её поровну.
 */
@Composable
fun WeekStrip(
    days: List<DayCell>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            DayItem(
                day = day,
                isSelected = day.date == selectedDate,
                onClick = { onSelect(day.date) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayItem(
    day: DayCell,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val circleColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "day-circle",
    )
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, RU).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                // круг занимает всю ширину ячейки — в референсе он крупный, почти впритык
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(circleColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor,
                )
                LessonDots(count = day.lessonCount, color = contentColor)
            }
        }
    }
}

@Composable
private fun LessonDots(count: Int, color: androidx.compose.ui.graphics.Color) {
    if (count == 0) {
        Spacer(Modifier.height(7.dp))
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 2.dp),
    ) {
        repeat(count.coerceAtMost(4)) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.7f)),
            )
        }
    }
}
