package ru.dzhaparidze.mykct.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.Selection
import ru.dzhaparidze.mykct.feature.NAV_BAR_INSET
import ru.dzhaparidze.mykct.feature.schedule.components.ActivityCard
import ru.dzhaparidze.mykct.feature.schedule.components.DayTimeline
import ru.dzhaparidze.mykct.feature.schedule.components.GroupSheet
import ru.dzhaparidze.mykct.feature.schedule.components.LessonSheet
import ru.dzhaparidze.mykct.feature.schedule.components.WeekStrip
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val RU = Locale.forLanguageTag("ru-RU")

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
    "${dayMonth()}, ${dayOfWeek.getDisplayName(TextStyle.FULL, RU)}"

/** На сколько белый лист контента наезжает на лавандовую шапку (радиус его верхних углов). */
private val SHEET_OVERLAP = 24.dp

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var groupSheetOpen by rememberSaveable { mutableStateOf(false) }
    var lessonSheet by remember { mutableStateOf<Lesson?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Header(
            groupLabel = state.selection.label(),
            onOpenGroups = { groupSheetOpen = true },
        )

        // Белый лист со скруглённым верхом поверх шапки — так же, как в референсе.
        Column(
            modifier = Modifier
                .offset(y = -SHEET_OVERLAP)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(topStart = SHEET_OVERLAP, topEnd = SHEET_OVERLAP),
                )
                .padding(top = 16.dp),
        ) {
            ActivityCard(
                title = "Прогресс дня",
                progress = state.progress,
                modifier = Modifier.padding(horizontal = 28.dp),
            )

            Spacer(Modifier.height(24.dp))

            WeekNav(
                weekStart = state.weekStart,
                onPrev = { viewModel.shiftWeek(-1) },
                onNext = { viewModel.shiftWeek(1) },
            )

            Spacer(Modifier.height(12.dp))

            WeekStrip(
                days = state.days,
                selectedDate = state.selectedDate,
                onSelect = viewModel::selectDate,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(24.dp))

            SectionRow(
                title = state.selectedDate.dayTitle().replaceFirstChar { it.uppercase() },
                onToday = viewModel::goToToday,
            )

            Spacer(Modifier.height(16.dp))

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
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    now = if (state.selectedDate == LocalDate.now()) LocalTime.now() else null,
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
 * Лавандовая шапка: слева название экрана, справа фильтр — он же показывает текущий выбор.
 * Переключение недели уехало вниз, к самой полосе дней: в шапке две голые стрелки читались
 * как «назад/вперёд» вообще, а не как «неделя».
 */
@Composable
private fun Header(
    groupLabel: String,
    onOpenGroups: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 24.dp + SHEET_OVERLAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Расписание",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )

        AssistChip(
            onClick = onOpenGroups,
            label = {
                Text(
                    text = groupLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Выбрать группу и подгруппы",
                    modifier = Modifier.size(18.dp),
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurface,
                trailingIconContentColor = MaterialTheme.colorScheme.primary,
            ),
            border = null,
            modifier = Modifier.widthIn(max = 200.dp),
        )
    }
}

/** Переключение недели: стрелки вплотную к диапазону дат, чтобы было видно, что листается. */
@Composable
private fun WeekNav(weekStart: LocalDate, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(Icons.Default.KeyboardArrowLeft, "Предыдущая неделя", onPrev)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Неделя",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = weekRange(weekStart),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }

        RoundButton(Icons.Default.KeyboardArrowRight, "Следующая неделя", onNext)
    }
}

/** «11 – 17 августа», а через границу месяца — «28 июля – 3 августа». */
private fun weekRange(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    val start = if (weekStart.month == end.month) weekStart.dayOfMonth.toString() else weekStart.dayMonth()
    return "$start – ${end.dayMonth()}"
}

/** Подпись дня и круглая кнопка «сегодня» — строка «Timeline» из референса. */
@Composable
private fun SectionRow(title: String, onToday: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        RoundButton(Icons.Default.DateRange, "Сегодня", onToday)
    }
}

/** Круглая кнопка на белом листе — обводка нужна, без неё surface с фоном не различается. */
@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
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
