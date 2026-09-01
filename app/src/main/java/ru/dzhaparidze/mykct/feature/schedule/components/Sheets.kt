package ru.dzhaparidze.mykct.feature.schedule.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.Groups
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.LessonSubgroup
import ru.dzhaparidze.mykct.data.Selection
import ru.dzhaparidze.mykct.data.detailLabel
import ru.dzhaparidze.mykct.data.selectedIds
import ru.dzhaparidze.mykct.data.sortedByLabel
import ru.dzhaparidze.mykct.feature.schedule.LessonDetails
import ru.dzhaparidze.mykct.ui.Fade
import ru.dzhaparidze.mykct.ui.Phase
import ru.dzhaparidze.mykct.ui.ShinyPill
import ru.dzhaparidze.mykct.ui.Swirl
import ru.dzhaparidze.mykct.ui.phaseOf
import ru.dzhaparidze.mykct.ui.hairline
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
 * по времени и наедет на следующую, поэтому всё здесь: шапка-карточка, повторяющая ту,
 * по которой тапнули, подгруппы (каждая раскрывается в свои детали по SClID) и то,
 * что отдал портал по GET /api/v1/classdetails.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonSheet(
    details: LessonDetails,
    selection: Selection,
    onSelectSubgroup: (LessonSubgroup?) -> Unit,
    onDismiss: () -> Unit,
) {
    val lesson = details.lesson
    val ownIds = selectedIds(selection)
    val rows = details.visibleRows()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            LessonHero(lesson)

            if (lesson.subgroups.isEmpty()) {
                SheetCard(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Подробности",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    DetailsContent(details, rows)
                }
            } else {
                Text(
                    text = "Подгруппы",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                )
                // Ключ по индексу, а не по SGrID: в одной паре он повторяется —
                // две «BE» в разных кабинетах у ИТ25-11 это живой случай.
                lesson.subgroups.forEachIndexed { index, subgroup ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    SubgroupCard(
                        subgroup = subgroup,
                        lessonTitle = lesson.title,
                        isOpen = details.selected == subgroup,
                        isOwn = subgroup.id in ownIds,
                        onClick = { onSelectSubgroup(if (details.selected == subgroup) null else subgroup) },
                    ) {
                        DetailsContent(details, rows)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Шапка листа — та же карточка пары, только крупнее: студент тапнул по фиолетовой
 * карточке, и лист продолжает её, а не открывает системный диалог с плоским текстом.
 */
@Composable
private fun LessonHero(lesson: Lesson) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AccentGradient),
    ) {
        Icon(
            painter = painterResource(subjectIcon(lesson.title)),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.13f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
                .size(84.dp),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            TimePill(
                text = "${lesson.start.format(SHEET_TIME)} - ${lesson.end.format(SHEET_TIME)}",
                showCheck = false,
                accent = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = lesson.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp),
            )

            if (lesson.topic.isNotBlank()) {
                Text(
                    text = lesson.topic,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (lesson.room.isNotBlank() || lesson.subgroups.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (lesson.room.isNotBlank()) {
                        Chip(text = lesson.room, icon = R.drawable.ic_place)
                    }
                    if (lesson.subgroups.isNotEmpty()) {
                        Chip(text = "Подгруппы: ${lesson.subgroups.size}", icon = R.drawable.ic_list)
                    }
                }
            }
        }
    }
}

/**
 * Подгруппа-аккордеон: детали приходят по её собственному SClID, поэтому раскрытие
 * живёт внутри карточки, а не открывает второй лист. Подгруппа без SClID не жмётся —
 * спрашивать по ней нечего.
 */
@Composable
private fun SubgroupCard(
    subgroup: LessonSubgroup,
    lessonTitle: String,
    isOpen: Boolean,
    isOwn: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val canOpen = subgroup.classId.isNotBlank()
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isOpen) colors.surfaceVariant else colors.surface)
            .hairline(shape)
            .clickable(enabled = canOpen, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = subgroup.id,
                style = MaterialTheme.typography.titleLarge,
                color = if (isOpen) colors.primary else colors.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.weight(1f))
            if (canOpen) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (isOpen) 180f else 0f),
                )
            }
        }

        if (subgroup.title.isNotBlank() && subgroup.title != lessonTitle) {
            Text(
                text = subgroup.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (subgroup.topic.isNotBlank()) {
            Text(
                text = subgroup.topic,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (subgroup.room.isNotBlank() || isOwn) {
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (subgroup.room.isNotBlank()) {
                    SheetChip(text = subgroup.room, icon = R.drawable.ic_place, accent = false)
                }
                // Своя подгруппа помечена: иначе её ищут глазами в списке из трёх-четырёх
                if (isOwn) {
                    SheetChip(text = "Ваша подгруппа", icon = R.drawable.ic_check, accent = true)
                }
            }
        }

        if (isOpen) {
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            content()
        }
    }
}

/** Детали пары или раскрытой подгруппы: индикатор, ошибка, «пусто» или сами строки. */
@Composable
private fun DetailsContent(details: LessonDetails, rows: List<Pair<String, String>>) {
    Fade(target = phaseOf(details.isLoading, details.error, rows.isEmpty())) { phase ->
        when (phase) {
            Phase.Loading -> Swirl(modifier = Modifier.size(24.dp))
            Phase.Error -> Caption(details.error ?: "")
            // Пустой ответ портала — это тоже ответ: без строки тап читается как «ничего не произошло»
            Phase.Empty -> Caption("Подробностей нет")
            Phase.Content -> rows.forEachIndexed { index, (key, value) ->
                Column(modifier = Modifier.padding(top = if (index == 0) 0.dp else 12.dp)) {
                    Text(
                        text = detailLabel(key),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * Строки, которые стоит показывать: то, что уже написано в шапке или на карточке
 * подгруппы, портал часто повторяет и в деталях — второй раз это шум.
 */
private fun LessonDetails.visibleRows(): List<Pair<String, String>> {
    val shown = listOfNotNull(lesson.title, lesson.topic, selected?.title, selected?.topic)
        .filter { it.isNotBlank() }
        .map { it.lowercase() }
        .toSet()
    return rows.sortedByLabel().filterNot { it.second.lowercase() in shown }
}

/** Карточка секции листа: та же поверхность с волосяной кромкой, что у подгрупп. */
@Composable
private fun SheetCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .hairline(shape)
            .padding(16.dp),
        content = content,
    )
}

/** Чип на поверхности листа: у [Chip] из карточки пары цвета белые, под градиент. */
@Composable
private fun SheetChip(text: String, @DrawableRes icon: Int, accent: Boolean) {
    val color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
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
