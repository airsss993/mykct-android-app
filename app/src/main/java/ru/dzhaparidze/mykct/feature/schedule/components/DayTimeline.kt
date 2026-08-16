package ru.dzhaparidze.mykct.feature.schedule.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.data.Lesson
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val SLOT_MINUTES = 30
private val SLOT_HEIGHT = 56.dp // полчаса в референсе — примерно треть карточки пары
private val GUTTER = 56.dp
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/**
 * Сетка времени с шагом 30 минут и карточки пар поверх неё:
 * высота карточки пропорциональна длительности пары, как в референсе.
 */
@Composable
fun DayTimeline(
    lessons: List<Lesson>,
    now: LocalTime?,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (lessons.isEmpty()) return

    val gridStart = lessons.first().start.truncatedTo(java.time.temporal.ChronoUnit.HOURS)
    val gridEnd = lessons.last().end.roundUpToSlot()
    val slots = (minutesBetween(gridStart, gridEnd) / SLOT_MINUTES).coerceAtLeast(1)

    Box(modifier = modifier.height(SLOT_HEIGHT * slots)) {
        repeat(slots + 1) { index ->
            SlotLine(
                time = gridStart.plusMinutes((index.toLong() * SLOT_MINUTES)),
                modifier = Modifier.offset(y = SLOT_HEIGHT * index.toFloat()),
            )
        }

        lessons.forEach { lesson ->
            val top = minutesBetween(gridStart, lesson.start).toFloat() / SLOT_MINUTES
            val span = minutesBetween(lesson.start, lesson.end).toFloat() / SLOT_MINUTES

            LessonCard(
                lesson = lesson,
                isPast = now != null && !lesson.end.isAfter(now),
                onClick = { onLessonClick(lesson) },
                modifier = Modifier
                    .offset(y = SLOT_HEIGHT * top)
                    .padding(start = GUTTER, bottom = 6.dp)
                    // ponytail: min, а не фиксированная высота — длинное название темы
                    // иначе обрежется. Растянувшаяся карточка может наехать на следующую;
                    // если начнёт мешать — резать текст по maxLines, а не жёстко фиксировать высоту.
                    .heightIn(min = SLOT_HEIGHT * span),
            )
        }
    }
}

@Composable
private fun SlotLine(time: LocalTime, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    // Линия рисуется ровно на отметке времени (Alignment.Top), подпись поднимается
    // на половину своей высоты, чтобы стоять по центру линии — иначе карточки,
    // которые позиционируются по той же отметке, разъедутся с сеткой.
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = time.format(TIME),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(GUTTER)
                .offset(y = (-8).dp),
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
        ) {
            drawLine(
                color = lineColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = size.height,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f)),
            )
        }
    }
}

private fun minutesBetween(from: LocalTime, to: LocalTime): Int =
    Duration.between(from, to).toMinutes().toInt().coerceAtLeast(0)

private fun LocalTime.roundUpToSlot(): LocalTime {
    val rest = minute % SLOT_MINUTES
    val rounded = if (rest == 0) this else plusMinutes((SLOT_MINUTES - rest).toLong())
    return rounded.withSecond(0).withNano(0)
}
