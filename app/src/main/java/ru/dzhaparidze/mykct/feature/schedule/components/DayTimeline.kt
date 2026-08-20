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

/**
 * Сетка времени с шагом 30 минут и карточки пар поверх неё:
 * высота карточки пропорциональна длительности пары, как в референсе.
 *
 * Перерывы не подписываются: пустое место в сетке и подписи времени слева говорят о
 * дыре сами, а плашка «Перерыв · 30 мин» между карточками спорила с ними за внимание.
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

        // Линия рисуется до карточек: иначе она проезжает поперёк текста идущей пары.
        // Подпись и точка живут в колонке времени, их карточка не закрывает.
        if (now != null && now >= gridStart && now <= gridEnd) {
            NowLine(
                modifier = Modifier.offset(
                    y = SLOT_HEIGHT * (minutesBetween(gridStart, now).toFloat() / SLOT_MINUTES),
                ),
                time = now,
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
                    // Отступа снизу быть не может: он съедал у карточки 6dp высоты,
                    // и её нижняя кромка не доходила до отметки конца пары.
                    .padding(start = GUTTER)
                    // ponytail: min, а не фиксированная высота — длинное название темы
                    // иначе обрежется. Растянувшаяся карточка может наехать на следующую;
                    // если начнёт мешать — резать текст по maxLines, а не жёстко фиксировать высоту.
                    .heightIn(min = SLOT_HEIGHT * span),
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
