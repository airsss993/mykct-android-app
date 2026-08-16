package ru.dzhaparidze.mykct.feature.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.dzhaparidze.mykct.data.Groups
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.Selection
import java.time.format.DateTimeFormatter

private val SHEET_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Выбор группы и подгрупп. Каталога в API нет, поэтому весь список — локальный. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSheet(
    selection: Selection,
    onSelect: (Selection) -> Unit,
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
            Section("Группа") {
                Groups.all.forEach { group ->
                    Choice(group, selection.group == group) { onSelect(selection.withGroup(group)) }
                }
            }

            Section("Подгруппа") {
                Choice("Все", selection.subgroup == null) { onSelect(selection.withSubgroup(null)) }
                Groups.subgroups(selection.group).forEach { subgroup ->
                    Choice(subgroup.title, selection.subgroup == subgroup.id) {
                        onSelect(selection.withSubgroup(subgroup.id))
                    }
                }
            }

            val profileSubgroups = Groups.profileSubgroups(selection.group, selection.subgroup)
            if (profileSubgroups.isNotEmpty()) {
                Section("Подгруппа профиля") {
                    Choice("Все", selection.profileSubgroup == null) {
                        onSelect(selection.copy(profileSubgroup = null))
                    }
                    profileSubgroups.forEach { subgroup ->
                        Choice(subgroup.title, selection.profileSubgroup == subgroup.id) {
                            onSelect(selection.copy(profileSubgroup = subgroup.id))
                        }
                    }
                }
            }

            Section("Английский") {
                Choice("Все", selection.englishGroup == null) { onSelect(selection.copy(englishGroup = null)) }
                Groups.englishGroups(selection.group).forEach { english ->
                    Choice(english, selection.englishGroup == english) {
                        onSelect(selection.copy(englishGroup = english))
                    }
                }
            }
        }
    }
}

/**
 * Пара, которая делится на подгруппы. Раскрывать карточку прямо в таймлайне нельзя —
 * она позиционируется по времени и наедет на следующую, поэтому подробности здесь.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonSheet(lesson: Lesson, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "${lesson.start.format(SHEET_TIME)} – ${lesson.end.format(SHEET_TIME)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )

            lesson.subgroups.forEach { subgroup ->
                HorizontalDivider(Modifier.padding(vertical = 16.dp))

                Text(
                    text = subgroup.id,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = subgroup.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp),
                )
                listOfNotNull(
                    subgroup.topic.takeIf { it.isNotBlank() },
                    subgroup.room.takeIf { it.isNotBlank() }?.let { "Кабинет $it" },
                ).forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow: в старых версиях foundation ещё experimental
@Composable
private fun Section(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}
