package ru.dzhaparidze.mykct.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.api.Attendance
import ru.dzhaparidze.mykct.data.api.AttendanceRecord
import ru.dzhaparidze.mykct.ui.NavArrow
import ru.dzhaparidze.mykct.ui.hairline
import ru.dzhaparidze.mykct.ui.theme.DangerFill
import ru.dzhaparidze.mykct.ui.theme.GreenFill
import ru.dzhaparidze.mykct.ui.theme.Ink
import ru.dzhaparidze.mykct.ui.theme.WarningFill
import ru.dzhaparidze.mykct.ui.theme.statusDanger
import ru.dzhaparidze.mykct.ui.theme.statusGreen
import ru.dzhaparidze.mykct.ui.theme.statusWarning
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val RU_CAL = Locale.forLanguageTag("ru-RU")

internal val MONTHS_NOMINATIVE = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

private val CELL_SHAPE = RoundedCornerShape(14.dp)

/**
 * Чем окрашен день в календаре. Порядок важнее числа: один прогул без уважительной
 * красит день красным, даже если остальные четыре пары посещены - именно он и есть
 * новость, а "3 из 4" в зелёном дне теряется.
 */
private enum class DayMark { NONE, PRESENT, EXCUSED, ABSENT }

private fun markOf(records: List<AttendanceRecord>): DayMark = when {
    records.any { it.attendance == Attendance.ABSENT } -> DayMark.ABSENT
    records.any { it.attendance == Attendance.EXCUSED } -> DayMark.EXCUSED
    records.any { it.attendance == Attendance.PRESENT } -> DayMark.PRESENT
    else -> DayMark.NONE
}

/** Цвет цифры и мягкой подложки: зависит от темы, на белом фоне нужны тёмные тона. */
@Composable
private fun DayMark.color(): Color? = when (this) {
    DayMark.PRESENT -> statusGreen
    DayMark.EXCUSED -> statusWarning
    DayMark.ABSENT -> statusDanger
    DayMark.NONE -> null
}

/** Заливка выбранного дня: одна на обе темы, текст поверх неё тёмный, а не белый. */
private fun DayMark.fill(): Color? = when (this) {
    DayMark.PRESENT -> GreenFill
    DayMark.EXCUSED -> WarningFill
    DayMark.ABSENT -> DangerFill
    DayMark.NONE -> null
}

/** Диагональная штриховка: ею закрыты дни соседних месяцев - сетка остаётся целой. */
private fun Modifier.hatch(color: Color): Modifier = drawBehind {
    val step = 7.dp.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, size.height),
            end = Offset(x + size.height, 0f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        x += step
    }
}

/**
 * Календарь месяца: заливка ячейки говорит, что было в этот день, а тап открывает
 * список пар под ним. Заменил кольцо с процентом - процент за неделю не отвечал на
 * вопрос "когда я пропустил", а календарь отвечает без единой цифры.
 */
@Composable
fun AttendanceCalendar(
    month: YearMonth,
    selected: LocalDate,
    records: List<AttendanceRecord>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val marks = remember(records) {
        records.groupBy { it.date }.mapValues { (_, day) -> markOf(day) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .hairline(RoundedCornerShape(24.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
                    .clickable(onClick = onToday),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = "Сегодня",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = "${MONTHS_NOMINATIVE[month.monthValue - 1]} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            NavArrow(R.drawable.ic_chevron_left, "Предыдущий месяц", onPrev)
            Spacer(Modifier.size(8.dp))
            NavArrow(R.drawable.ic_chevron_right, "Следующий месяц", onNext)
        }

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, RU_CAL)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Месяц уезжает в ту сторону, куда листнули: без движения смена читается
        // как перерисовка, а не как переход к соседнему месяцу.
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val dx = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(220)) { dx * it / 5 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { -dx * it / 5 } + fadeOut(tween(160)))
            },
            label = "month",
        ) { shown ->
            MonthGrid(shown, selected, marks, onSelect)
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    marks: Map<LocalDate, DayMark>,
    onSelect: (LocalDate) -> Unit,
) {
    // Неделя начинается с понедельника, поэтому отступ считается от него, а не от
    // воскресенья: `DayOfWeek.value` даёт 1 для понедельника.
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val rows = (lead + month.lengthOfMonth() + 6) / 7
    val today = LocalDate.now()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - lead + 1
                    if (dayNumber < 1 || dayNumber > month.lengthOfMonth()) {
                        OutsideCell()
                    } else {
                        val date = month.atDay(dayNumber)
                        DayCell(
                            day = dayNumber,
                            mark = marks[date] ?: DayMark.NONE,
                            isSelected = date == selected,
                            isToday = date == today,
                            onClick = { onSelect(date) },
                        )
                    }
                }
            }
        }
    }
}

/** Ячейка соседнего месяца: кликать нечего, но место занять надо - иначе сетка поедет. */
@Composable
private fun RowScope.OutsideCell() {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(CELL_SHAPE)
            .hatch(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
    )
}

@Composable
private fun RowScope.DayCell(
    day: Int,
    mark: DayMark,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val accent = mark.color()
    val solid = mark.fill()
    val fill = when {
        // Выбранный день залит целиком - как в референсе. Цвет берётся от отметки,
        // чтобы выбор не стирал то, ради чего в календарь и смотрят.
        isSelected -> solid ?: MaterialTheme.colorScheme.primary
        accent != null -> accent.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val text = when {
        // На залитом дне текст тёмный: белый на жёлтой заливке даёт 1.6:1.
        // День без отметки залит акцентом - там наоборот, тёмный не читается.
        isSelected -> if (solid != null) Ink else Color.White
        accent != null -> accent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(CELL_SHAPE)
            .background(fill)
            .then(
                // Сегодняшний день обведён, пока не выбран: заливкой его не отметить,
                // она уже занята статусом.
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CELL_SHAPE)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = text,
        )
    }
}
