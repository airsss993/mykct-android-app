package ru.dzhaparidze.mykct.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.data.api.Score
import ru.dzhaparidze.mykct.data.api.Subject
import ru.dzhaparidze.mykct.data.api.SubjectLesson
import ru.dzhaparidze.mykct.ui.hairline
import ru.dzhaparidze.mykct.ui.theme.Danger
import ru.dzhaparidze.mykct.ui.theme.Green
import ru.dzhaparidze.mykct.ui.theme.Warning

/**
 * Баллы по предмету за текущее полугодие. Оценка приходит строкой и бывает пустой —
 * это «ещё не оценено», а не ноль, поэтому такие занятия показываем отдельной подписью.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresSheet(
    subject: Subject,
    lessons: List<SubjectLesson>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = subject.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val graded = lessons.flatMap { it.scores }.mapNotNull { it.value }
            if (graded.isNotEmpty()) {
                Text(
                    text = "Средний балл ${"%.1f".format(graded.average())} · оценок ${graded.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                error != null -> Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                lessons.isEmpty() -> Text(
                    text = "За это полугодие баллов нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> lessons.forEach { lesson -> LessonScores(lesson) }
            }
        }
    }
}

@Composable
private fun LessonScores(lesson: SubjectLesson) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        if (lesson.title.isNotBlank()) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        lesson.scores.forEach { score -> ScoreRow(score) }
    }
}

@Composable
private fun ScoreRow(score: Score) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .hairline(RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = score.description.ifBlank { "Без описания" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            score.date?.let {
                Text(
                    text = "${it.dayOfMonth}.${"%02d".format(it.monthValue)}.${it.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = score.value?.let { "$it / ${score.max}" } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = score.color(),
        )
    }
}

/** Цвет оценки от доли максимума: как в iOS — зелёный/оранжевый/красный. */
@Composable
private fun Score.color(): Color {
    val value = this.value ?: return MaterialTheme.colorScheme.onSurfaceVariant
    val share = if (max > 0) value.toDouble() / max else 0.0
    return when {
        share >= 0.8 -> Green
        share >= 0.6 -> Warning
        else -> Danger
    }
}
