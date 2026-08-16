package ru.dzhaparidze.mykct.feature.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import ru.dzhaparidze.mykct.feature.schedule.components.ActivityCard
import ru.dzhaparidze.mykct.feature.schedule.components.DayTimeline
import ru.dzhaparidze.mykct.feature.schedule.components.GroupSheet
import ru.dzhaparidze.mykct.feature.schedule.components.LessonSheet
import ru.dzhaparidze.mykct.feature.schedule.components.WeekStrip
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU = Locale.forLanguageTag("ru-RU")
private val DAY_TITLE = DateTimeFormatter.ofPattern("d MMMM, EEEE", RU)

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
            onPrevWeek = { viewModel.shiftWeek(-1) },
            onNextWeek = { viewModel.shiftWeek(1) },
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

            WeekStrip(
                days = state.days,
                selectedDate = state.selectedDate,
                onSelect = viewModel::selectDate,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(24.dp))

            SectionRow(
                title = state.selectedDate.format(DAY_TITLE).replaceFirstChar { it.uppercase() },
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

            Spacer(Modifier.height(32.dp))
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
 * Лавандовая шапка из референса: круглые кнопки по краям, по центру — что показываем.
 * В референсе по центру название экрана, но у нас важнее группа: экран всё равно один,
 * а группу иначе негде показать и негде сменить.
 */
@Composable
private fun Header(
    groupLabel: String,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
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
        RoundButton(Icons.Default.KeyboardArrowLeft, "Предыдущая неделя", onPrevWeek)

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenGroups)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = groupLabel,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Выбрать группу",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        RoundButton(Icons.Default.KeyboardArrowRight, "Следующая неделя", onNextWeek)
    }
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

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onToday),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Сегодня",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
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
