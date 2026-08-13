package ru.dzhaparidze.mykct.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dzhaparidze.mykct.feature.schedule.components.ActivityCard
import ru.dzhaparidze.mykct.feature.schedule.components.DayTimeline
import ru.dzhaparidze.mykct.feature.schedule.components.WeekStrip
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU = Locale.forLanguageTag("ru-RU")
private val DAY_TITLE = DateTimeFormatter.ofPattern("d MMMM, EEEE", RU)

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Header(
            title = "Расписание",
            onPrevWeek = { viewModel.shiftWeek(-1) },
            onNextWeek = { viewModel.shiftWeek(1) },
            card = {
                ActivityCard(
                    title = "Прогресс дня",
                    subtitle = activitySubtitle(state),
                    progress = state.progress,
                )
            },
        )

        Spacer(Modifier.height(52.dp)) // компенсирует наезд карточки на шапку

        WeekStrip(
            days = state.days,
            selectedDate = state.selectedDate,
            onSelect = viewModel::selectDate,
            onToday = viewModel::goToToday,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = state.selectedDate.format(DAY_TITLE).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
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
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Header(
    title: String,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    card: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 40.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton(Icons.Default.KeyboardArrowLeft, "Предыдущая неделя", onPrevWeek)
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                RoundButton(Icons.Default.KeyboardArrowRight, "Следующая неделя", onNextWeek)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 52.dp)
                .padding(horizontal = 16.dp),
        ) {
            card()
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
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

private fun activitySubtitle(state: ScheduleUiState): String = when {
    state.lessons.isEmpty() -> "Пар нет"
    else -> "${state.passedCount} из ${state.lessons.size} пар прошло"
}
