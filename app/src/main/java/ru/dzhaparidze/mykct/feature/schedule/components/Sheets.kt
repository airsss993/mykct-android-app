package ru.dzhaparidze.mykct.feature.schedule.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import ru.dzhaparidze.mykct.data.Groups
import ru.dzhaparidze.mykct.data.Selection
import ru.dzhaparidze.mykct.feature.schedule.LessonDetails
import ru.dzhaparidze.mykct.ui.Fade
import ru.dzhaparidze.mykct.ui.Phase
import ru.dzhaparidze.mykct.ui.ShinyPill
import ru.dzhaparidze.mykct.ui.Swirl
import ru.dzhaparidze.mykct.ui.phaseOf
import ru.dzhaparidze.mykct.ui.theme.AccentGradient

private val SHEET_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/**
 * Выбор группы и подгрупп. Каталога в API нет, поэтому весь список — локальный.
 *
 * Группы разложены по наборам, а не единой простынёй из тринадцати чипов: номер набора
 * — первое, что студент ищет глазами. Лист открывается сразу во всю высоту
 * (`skipPartiallyExpanded`), иначе «Английский» оказывался за двумя прокрутками.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSheet(
    selection: Selection,
    onSelect: (Selection) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = "Моя группа",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        // Строка выбора целиком: по одним чипам не видно, что уже выбрано в свёрнутых
        // ниже секциях, а именно её студент и сверяет с расписанием.
        Text(
            text = summary(selection),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Section("Группа") {
                Groups.bySet.forEach { (year, groups) ->
                    Text(
                        text = "Набор $year",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                    )
                    Chips {
                        groups.forEach { group ->
                            Choice(group, selection.group == group) {
                                onSelect(selection.withGroup(group))
                            }
                        }
                    }
                }
            }

            Section("Подгруппа") {
                Chips {
                    Choice("Все", selection.subgroup == null) { onSelect(selection.withSubgroup(null)) }
                    Groups.subgroups(selection.group).forEach { subgroup ->
                        Choice(subgroup.title, selection.subgroup == subgroup.id) {
                            onSelect(selection.withSubgroup(subgroup.id))
                        }
                    }
                }
            }

            val profileSubgroups = Groups.profileSubgroups(selection.group, selection.subgroup)
            if (profileSubgroups.isNotEmpty()) {
                Section("Подгруппа профиля") {
                    Chips {
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
            }

            Section("Английский") {
                Chips {
                    Choice("Все", selection.englishGroup == null) {
                        onSelect(selection.copy(englishGroup = null))
                    }
                    Groups.englishGroups(selection.group).forEach { english ->
                        Choice(english, selection.englishGroup == english) {
                            onSelect(selection.copy(englishGroup = english))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Выбор применяется сразу, кнопка только закрывает лист — но без неё непонятно,
        // что делать дальше: свайп вниз как единственный выход студенты не находят.
        ShinyPill(
            text = "Готово",
            onClick = onDismiss,
            shine = false,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        )

        Spacer(Modifier.navigationBarsPadding())
    }
}

/** Что выбрано сейчас, одной строкой: «ИТ25-11 · Подгруппа 1 · A1.11». */
private fun summary(selection: Selection): String = listOfNotNull(
    selection.group,
    selection.subgroup?.let { id ->
        Groups.subgroups(selection.group).firstOrNull { it.id == id }?.title ?: id
    },
    selection.profileSubgroup?.let { id ->
        Groups.profileSubgroups(selection.group, selection.subgroup)
            .firstOrNull { it.id == id }?.title ?: id
    },
    selection.englishGroup,
).joinToString(" · ")

/**
 * Подробности пары. Раскрывать карточку прямо в таймлайне нельзя — она позиционируется
 * по времени и наедет на следующую, поэтому всё здесь: подгруппы, если пара делится,
 * и то, что отдал портал по GET /api/v1/classdetails.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonSheet(details: LessonDetails, onDismiss: () -> Unit) {
    val lesson = details.lesson
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            // У пары без подгрупп тема и кабинет лежат в ней самой — иначе лист пустой
            listOfNotNull(
                lesson.topic.takeIf { it.isNotBlank() },
                lesson.room.takeIf { it.isNotBlank() }?.let { "Кабинет $it" },
            ).forEach { Caption(it) }

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
                ).forEach { Caption(it) }
            }

            // Молча ничего не показываем, если портал ничего не дал: пустая секция
            // «Подробности» на каждой паре выглядела бы поломкой.
            if (details.isLoading || details.error != null || details.rows.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text(
                    text = "Подробности",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Fade(target = phaseOf(details.isLoading, details.error)) { phase ->
                    when (phase) {
                        Phase.Loading -> Swirl(
                            modifier = Modifier.padding(top = 12.dp).size(20.dp),
                        )
                        Phase.Error -> Caption(details.error ?: "")
                        // Ключи сырые: схемы у /classdetails нет, см. flattenDetails
                        else -> details.rows.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
    Column(content = content)
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow: в старых версиях foundation ещё experimental
@Composable
private fun Chips(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * Чип выбора. Не `FilterChip`: у материального выбранное состояние — бледная заливка,
 * на светлой теме её почти не видно. Здесь выбранный залит фирменным градиентом,
 * как выбранный день в полосе недели, — и переключается так же: градиент проявляется
 * поверх нейтральной заливки, цвет текста доезжает по lerp.
 */
@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "choice-selected",
    )
    val contentColor = lerp(MaterialTheme.colorScheme.onSurface, Color.White, progress)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // Ряби нет: отклик — сама заливка, на градиенте она читается грязью.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(progress)
                .background(AccentGradient),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
        )
    }
}
