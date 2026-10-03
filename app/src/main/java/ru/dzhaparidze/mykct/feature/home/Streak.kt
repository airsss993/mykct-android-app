package ru.dzhaparidze.mykct.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dzhaparidze.mykct.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dzhaparidze.mykct.data.api.Leaderboard
import ru.dzhaparidze.mykct.data.api.LeaderboardEntry
import ru.dzhaparidze.mykct.ui.Fade
import ru.dzhaparidze.mykct.ui.HeroSummary
import ru.dzhaparidze.mykct.ui.SegmentedSwitch
import ru.dzhaparidze.mykct.ui.Swirl
import ru.dzhaparidze.mykct.data.api.Attendance
import ru.dzhaparidze.mykct.data.api.AttendanceRecord
import ru.dzhaparidze.mykct.data.api.Streak
import ru.dzhaparidze.mykct.ui.hairline
import ru.dzhaparidze.mykct.ui.theme.AccentGradient
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Цвета огонька как в iOS: оранжево-красный живой, серый - когда стрика нет. */
private val FlameOrange = Color(0xFFFF9500)
private val FlameRed = Color(0xFFFF3B30)
private val FlameIdle = Color(0xFF8E8E93)

/**
 * Огонёк стрика. Живой (в листе) - блик бежит по градиенту, ореол дышит; статичный
 * (шапка, строки рейтинга) - один кадр без ореола: в шапке анимация отвлекала.
 */
@Composable
internal fun Flame(diameter: Dp, modifier: Modifier = Modifier, animated: Boolean = true, active: Boolean = true) {
    val colors = if (active) listOf(FlameOrange, FlameRed) else listOf(FlameIdle, FlameIdle.copy(alpha = 0.7f))
    val tint = if (active) FlameOrange else FlameIdle

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        if (animated) {
            val infinite = rememberInfiniteTransition(label = "streak")
            val shift by infinite.animateFloat(0f, 1f, wave(3600), label = "shift")
            val glow by infinite.animateFloat(0.3f, 0.48f, wave(4200), label = "glow")
            val spread by infinite.animateFloat(0.42f, 0.48f, wave(6500), label = "spread")
            Box(
                modifier = Modifier.matchParentSize().drawBehind {
                    val radius = size.minDimension * spread
                    drawCircle(
                        // Мягкий спад: на двух остановках у ореола видна кромка.
                        brush = Brush.radialGradient(
                            0f to tint.copy(alpha = glow),
                            0.4f to tint.copy(alpha = glow * 0.5f),
                            0.75f to tint.copy(alpha = glow * 0.16f),
                            1f to Color.Transparent,
                            radius = radius,
                        ),
                        radius = radius,
                    )
                },
            )
            FlameIcon(diameter, colors) { shift }
        } else {
            FlameIcon(diameter, colors) { 0.5f }
        }
    }
}

@Composable
private fun FlameIcon(diameter: Dp, colors: List<Color>, shift: () -> Float) {
    Image(
        painter = painterResource(R.drawable.ic_fire),
        contentDescription = null,
        modifier = Modifier
            .size(diameter * 0.5f)
            // SrcIn красит непрозрачные пиксели иконки градиентом, но только
            // в своём слое - без offscreen он затрёт всё, что нарисовано ниже.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val value = shift()
                drawRect(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, size.height * (value * 0.4f - 0.2f)),
                        end = Offset(0f, size.height * (value * 0.4f + 0.8f)),
                    ),
                    blendMode = BlendMode.SrcIn,
                )
            },
    )
}

/** Синусоида туда-обратно с периодом [periodMillis], как `wave` в iOS. */
private fun wave(periodMillis: Int) =
    infiniteRepeatable<Float>(tween(periodMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse)

/**
 * Кнопка-огонёк в углу шапки. Видна каждому вошедшему, даже пока стрик грузится;
 * серая, если стрика нет. Ряби нет намеренно.
 */
@Composable
fun StreakFlame(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Flame(
        // Ровно по высоте строки заголовка, как пилюля группы
        diameter = 48.dp,
        animated = false,
        active = active,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClickLabel = "Стрик посещений",
            onClick = onClick,
        ),
    )
}

private val STREAK_TABS = listOf("Стрик", "Рейтинг")

/**
 * Лист огонька: стрик и рейтинг курса. Данные стрика те же, что на "Главной"
 * ([HomeViewModel] один на приложение); рейтинг грузится по открытию вкладки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakSheet(viewModel: HomeViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    if (!state.canSeeLeaderboard && tab != 0) tab = 0
    LaunchedEffect(tab) { if (tab == 1) viewModel.loadLeaderboard() }

    // Лист открывается сразу целиком: в половинном состоянии видно только огонёк.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            if (state.canSeeLeaderboard) {
                SegmentedSwitch(items = STREAK_TABS, selected = tab, onSelect = { tab = it })
                Spacer(Modifier.height(24.dp))
            }
            Fade(target = tab) { current ->
                if (current == 0) StreakTab(state, viewModel) else LeaderboardTab(state.leaderboard, viewModel)
            }
        }
    }
}

@Composable
private fun StreakTab(state: HomeUiState, viewModel: HomeViewModel) {
    val streak = state.streak
    when {
        streak != null -> StreakContent(streak, state)
        state.isLoading -> Placeholder { Swirl() }
        else -> ErrorBlock("Не удалось загрузить стрик", retry = "Обновить", onRetry = viewModel::refresh)
    }
}

@Composable
private fun StreakContent(streak: Streak, state: HomeUiState) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Flame(diameter = 132.dp, active = streak.current > 0)

        Text(
            text = streak.current.toString(),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = daysInRow(streak.current, streak.schoolDays),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = status(streak.rate, streak.schoolDays),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
        WeekChecks(weekStart = state.weekStart, records = state.records)
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .hairline(RoundedCornerShape(20.dp))
                .padding(vertical = 16.dp),
        ) {
            Stat("Дней", streak.daysAttended.toString(), Modifier.weight(1f))
            Stat("Учебных", streak.schoolDays.toString(), Modifier.weight(1f))
            Stat("Посещал", "${streak.rate.toInt()}%", Modifier.weight(1f))
            Stat("Лучший", streak.longest.toString(), Modifier.weight(1f))
        }

        val start = streak.periodStart
        if (start != null && streak.schoolDays > 0) {
            Text(
                text = "Считаем с ${start.dayMonth()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun LeaderboardTab(feed: LeaderboardFeed, viewModel: HomeViewModel) {
    Fade(target = feed::class) { _ ->
        when (feed) {
            LeaderboardFeed.Idle, LeaderboardFeed.Loading -> Placeholder { Swirl() }
            is LeaderboardFeed.Failed -> ErrorBlock(feed.message, retry = "Обновить", onRetry = viewModel::reloadLeaderboard)
            LeaderboardFeed.Unavailable -> Placeholder {
                Text(
                    text = "Рейтинг пока недоступен",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            is LeaderboardFeed.Loaded -> LeaderboardBoard(feed.board)
        }
    }
}

@Composable
private fun LeaderboardBoard(board: Leaderboard) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HeroSummary(
            caption = "Твоё место среди ${students(board.participants)} курса",
            value = "${board.me.rank} место",
            subtitle = board.me.alias,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            board.top.forEach { LeaderboardRow(it) }
            if (!board.isMeInTop) {
                Text(
                    text = "...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                )
                LeaderboardRow(board.me)
            }
        }
        Text(
            text = "Рейтинг анонимный: за псевдонимами не видно ни имён, ни групп",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val shape = RoundedCornerShape(20.dp)
    val primary = MaterialTheme.colorScheme.primary
    val foreground = if (entry.isMe) Color.White else MaterialTheme.colorScheme.onSurface
    val place = "${entry.rank} место, ${entry.alias}, ${days(entry.streak)} подряд"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (entry.isMe) primary else MaterialTheme.colorScheme.surface, shape)
            .hairline(shape)
            .padding(16.dp)
            .clearAndSetSemantics { contentDescription = if (entry.isMe) "Ты: $place" else place },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (entry.isMe) Color.White.copy(alpha = 0.22f) else primary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.rank.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (entry.isMe) Color.White else primary,
                maxLines = 1,
            )
        }
        Text(
            text = entry.alias,
            style = MaterialTheme.typography.bodyLarge,
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.streak.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = foreground,
            )
            Flame(diameter = 22.dp, animated = false, active = entry.streak > 0)
        }
    }
}

@Composable
private fun Placeholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** "1 студента", "87 студентов": после "среди" - родительный падеж. */
private fun students(count: Int): String {
    val word = when {
        count % 100 in 11..14 -> "студентов"
        count % 10 == 1 -> "студента"
        else -> "студентов"
    }
    return "$count $word"
}

/**
 * Неделя как в референсе: день, на котором был, — кружок с галочкой, остальные —
 * просто число. Отметки берём из посещаемости той же недели, что открыта на «Главной».
 */
@Composable
private fun WeekChecks(weekStart: LocalDate, records: List<AttendanceRecord>) {
    val today = LocalDate.now()
    val byDate = records.groupBy { it.date }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (0..6).forEach { offset ->
            val date = weekStart.plusDays(offset.toLong())
            val attended = byDate[date].orEmpty().any { it.attendance == Attendance.PRESENT }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, RU_LOCALE).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .then(
                            if (attended) Modifier.background(AccentGradient, CircleShape) else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (attended) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_bold),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                            color = if (date > today) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val RU_LOCALE: Locale = Locale.forLanguageTag("ru-RU")

/** "5 дней подряд". Ноль - отдельный текст, "0 дней подряд" звучит зло. */
private fun daysInRow(count: Int, schoolDays: Int): String = when {
    schoolDays == 0 -> "Стрик ещё не начался"
    count == 0 -> "Стрик прервался"
    else -> "${days(count)} подряд"
}

private fun status(rate: Double, schoolDays: Int): String = when {
    schoolDays == 0 -> "Учебных дней пока не было"
    rate >= 90 -> "Ходишь почти без пропусков - так держать"
    rate >= 75 -> "Крепкая посещаемость, всё под контролем"
    rate >= 50 -> "Бывает по-разному - можно лучше"
    else -> "Пропусков много, пора возвращаться"
}
