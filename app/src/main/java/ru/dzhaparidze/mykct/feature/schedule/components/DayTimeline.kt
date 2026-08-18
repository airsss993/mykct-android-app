package ru.dzhaparidze.mykct.feature.schedule.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.data.Lesson
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val SLOT_MINUTES = 30
// Полчаса сетки. Задаёт высоту карточек: пара в 90 минут = три слота. Меньше 50.dp
// не опускать — содержимое карточки перестанет влезать и она вылезет из своего слота.
private val SLOT_HEIGHT = 50.dp
private val GUTTER = 56.dp
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Десятиминутные перемены подписывать незачем — плашка в них всё равно не влезет. */
private const val GAP_LABEL_MINUTES = 20

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
            val slotTime = gridStart.plusMinutes(index.toLong() * SLOT_MINUTES)
            SlotLine(
                time = slotTime,
                // Подпись сетки прячется, если рядом стоит «сейчас» — иначе цифры
                // наезжают друг на друга.
                showLabel = now == null || minutesBetween(minOf(now, slotTime), maxOf(now, slotTime)) > 12,
                modifier = Modifier.offset(y = SLOT_HEIGHT * index.toFloat()),
            )
        }

        lessons.forEach { lesson ->
            val top = minutesBetween(gridStart, lesson.start).toFloat() / SLOT_MINUTES
            val span = minutesBetween(lesson.start, lesson.end).toFloat() / SLOT_MINUTES

            val isNow = now != null && !now.isBefore(lesson.start) && now.isBefore(lesson.end)

            LessonCard(
                lesson = lesson,
                isPast = now != null && !lesson.end.isAfter(now),
                isNow = isNow,
                remaining = if (isNow) minutesBetween(now!!, lesson.end) else null,
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

        // Линия «сейчас» рисуется последней — она должна лежать поверх карточек.
        if (now != null && now >= gridStart && now <= gridEnd) {
            NowLine(
                modifier = Modifier.offset(
                    y = SLOT_HEIGHT * (minutesBetween(gridStart, now).toFloat() / SLOT_MINUTES),
                ),
                time = now,
            )
        }

        for ((previous, next) in lessons.zipWithNext()) {
            val gap = minutesBetween(previous.end, next.start)
            if (gap < GAP_LABEL_MINUTES) continue

            val top = minutesBetween(gridStart, previous.end).toFloat() / SLOT_MINUTES
            GapLabel(
                minutes = gap,
                modifier = Modifier
                    .offset(y = SLOT_HEIGHT * top)
                    .padding(start = GUTTER)
                    .height(SLOT_HEIGHT * (gap.toFloat() / SLOT_MINUTES)),
            )
        }
    }
}

/** Отметка текущего времени: подпись слева, точка и линия поперёк таймлайна. */
@Composable
private fun NowLine(time: LocalTime, modifier: Modifier = Modifier) {
    // Не primary: карточки пар сами фиолетовые, акцент на них не читается.
    val accent = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time.format(TIME),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.width(GUTTER),
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, CircleShape),
        )
        Canvas(modifier = Modifier.weight(1f).height(2.dp)) {
            drawLine(
                color = accent,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
            )
        }
    }
}

/**
 * Окно между парами: обеденные полчаса или дыра в расписании. Подпись нейтральная —
 * «обед» это только у стандартного перерыва, а дыра в час обедом не является.
 */
@Composable
private fun GapLabel(minutes: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "Перерыв · ${formatMinutes(minutes)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes мин"
    minutes % 60 == 0 -> "${minutes / 60} ч"
    else -> "${minutes / 60} ч ${minutes % 60} мин"
}

@Composable
private fun SlotLine(time: LocalTime, showLabel: Boolean, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    // Линия рисуется ровно на отметке времени (Alignment.Top), подпись поднимается
    // на половину своей высоты, чтобы стоять по центру линии — иначе карточки,
    // которые позиционируются по той же отметке, разъедутся с сеткой.
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = if (showLabel) time.format(TIME) else "",
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
