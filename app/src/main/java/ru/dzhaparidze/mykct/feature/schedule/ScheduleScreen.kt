package ru.dzhaparidze.mykct.feature.schedule

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.Selection
import ru.dzhaparidze.mykct.feature.NAV_BAR_INSET
import ru.dzhaparidze.mykct.feature.schedule.components.DayTimeline
import ru.dzhaparidze.mykct.feature.schedule.components.GroupSheet
import ru.dzhaparidze.mykct.feature.schedule.components.LessonSheet
import ru.dzhaparidze.mykct.feature.schedule.components.WeekStrip
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val RU = Locale.forLanguageTag("ru-RU")
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/**
 * Родительный падеж руками: `MMMM` на Android отдаёт именительный («16 август»),
 * а нужен «16 августа». Своего формата под это в java.time нет.
 */
private val MONTHS_GENITIVE = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

private fun LocalDate.dayMonth(): String = "$dayOfMonth ${MONTHS_GENITIVE[monthValue - 1]}"

private fun LocalDate.dayTitle(): String =
    "${dayOfWeek.getDisplayName(TextStyle.FULL, RU).replaceFirstChar { it.uppercase() }}, ${dayMonth()}"

/** «11 – 17 августа», а через границу месяца — «28 июля – 3 августа». */
private fun weekRange(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    val start = if (weekStart.month == end.month) weekStart.dayOfMonth.toString() else weekStart.dayMonth()
    return "$start – ${end.dayMonth()}"
}

/** «1 пара», «4 пары», «11 пар». */
private fun lessonsCount(count: Int): String {
    val word = when {
        count % 100 in 11..14 -> "пар"
        count % 10 == 1 -> "пара"
        count % 10 in 2..4 -> "пары"
        else -> "пар"
    }
    return "$count $word"
}

/** На сколько лист контента наезжает на градиентную шапку (радиус его верхних углов). */
private val SHEET_OVERLAP = 28.dp

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Часы тикают сами: линия «сейчас» и остаток пары должны ехать без перезахода
    // на экран. Полминуты — предел, при котором минуты на экране не врут.
    val now by produceState(LocalTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalTime.now()
        }
    }
    var groupSheetOpen by rememberSaveable { mutableStateOf(false) }
    var lessonSheet by remember { mutableStateOf<Lesson?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(
            state = state,
            onOpenGroups = { groupSheetOpen = true },
            onPrevWeek = { viewModel.shiftWeek(-1) },
            onNextWeek = { viewModel.shiftWeek(1) },
            onToday = viewModel::goToToday,
            onRefresh = viewModel::retry,
        )

        // Лист контента со скруглённым верхом наезжает на градиент — так же, как в референсе.
        Column(
            modifier = Modifier
                .offset(y = -SHEET_OVERLAP)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(topStart = SHEET_OVERLAP, topEnd = SHEET_OVERLAP),
                )
                .padding(top = 24.dp),
        ) {
            WeekStrip(
                days = state.days,
                selectedDate = state.selectedDate,
                onSelect = viewModel::selectDate,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(24.dp))

            when {
                state.isLoading -> Placeholder { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                state.error -> Placeholder {
                    Text(
                        text = "Не удалось загрузить расписание",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::retry) {
                        Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Повторить")
                    }
                }

                state.lessons.isEmpty() -> Placeholder {
                    Text(
                        text = "Пар нет",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Отдыхай",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> DayTimeline(
                    lessons = state.lessons,
                    now = if (state.selectedDate == LocalDate.now()) now else null,
                    onLessonClick = { lessonSheet = it },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(NAV_BAR_INSET))
        }
    }

    if (groupSheetOpen) {
        GroupSheet(
            selection = state.selection,
            onSelect = viewModel::updateSelection,
            onDismiss = { groupSheetOpen = false },
        )
    }

    lessonSheet?.let { lesson ->
        LessonSheet(lesson = lesson, onDismiss = { lessonSheet = null })
    }
}

/**
 * Градиентная шапка по референсу: строка заголовка с пилюлей выбора, крупная сводка дня
 * на месте «баланса» и ряд круглых действий под ней.
 */
@Composable
private fun Hero(
    state: ScheduleUiState,
    onOpenGroups: () -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentGradient)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp + SHEET_OVERLAP),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Расписание",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            GroupPill(label = state.selection.label(), onClick = onOpenGroups)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Неделя ${weekRange(state.weekStart)}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f),
        )
        Text(
            // Крупная строка на месте баланса из референса: сколько пар в выбранном дне.
            // Во время загрузки «Пар нет» — враньё, пока данных ещё нет.
            text = when {
                state.isLoading -> "Загружаем…"
                state.error -> "Нет данных"
                state.lessons.isEmpty() -> "Пар нет"
                else -> lessonsCount(state.lessons.size)
            },
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = state.selectedDate.dayTitle() + state.lessons.dayHours(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroAction(R.drawable.ic_calendar, "Сегодня", onToday, Modifier.weight(1f))
            HeroAction(R.drawable.ic_chevron_left, "Назад", onPrevWeek, Modifier.weight(1f), "Предыдущая неделя")
            HeroAction(R.drawable.ic_chevron_right, "Вперёд", onNextWeek, Modifier.weight(1f), "Следующая неделя")
            HeroAction(R.drawable.ic_refresh, "Обновить", onRefresh, Modifier.weight(1f))
        }
    }
}

/** « · 9:00 – 15:40» для непустого дня, иначе ничего. */
private fun List<Lesson>.dayHours(): String =
    if (isEmpty()) "" else " · ${first().start.format(TIME)} – ${last().end.format(TIME)}"

/** Стеклянная пилюля поверх градиента: показывает выбор и открывает шит групп. */
@Composable
private fun GroupPill(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.20f))
            .clickable(onClick = onClick)
            .widthIn(max = 200.dp)
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_down),
            contentDescription = "Выбрать группу и подгруппы",
            tint = Color.White,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(18.dp),
        )
    }
}

/** Круглая полупрозрачная кнопка с подписью — ряд действий из референса. */
@Composable
private fun HeroAction(
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
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Placeholder(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

private fun Selection.label(): String =
    listOfNotNull(group, subgroup, profileSubgroup, englishGroup).joinToString(" · ")
