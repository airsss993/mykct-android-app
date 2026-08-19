package ru.dzhaparidze.mykct.feature.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.api.Attendance
import ru.dzhaparidze.mykct.data.api.AttendanceRecord
import ru.dzhaparidze.mykct.data.api.Subject
import ru.dzhaparidze.mykct.feature.navBarInset
import ru.dzhaparidze.mykct.feature.schedule.drawAmbientGlow
import ru.dzhaparidze.mykct.ui.ShinyPill
import ru.dzhaparidze.mykct.ui.dotGrid
import ru.dzhaparidze.mykct.ui.hairline
import ru.dzhaparidze.mykct.ui.theme.Danger
import ru.dzhaparidze.mykct.ui.theme.Green
import ru.dzhaparidze.mykct.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val RU = Locale.forLanguageTag("ru-RU")
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

private val MONTHS_GENITIVE = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

internal fun LocalDate.dayMonth() = "$dayOfMonth ${MONTHS_GENITIVE[monthValue - 1]}"

private fun LocalDate.dayTitle() =
    "${dayOfWeek.getDisplayName(TextStyle.FULL, RU).replaceFirstChar { it.uppercase() }}, ${dayMonth()}"

private fun weekRange(monday: LocalDate): String {
    val end = monday.plusDays(6)
    val start = if (monday.month == end.month) monday.dayOfMonth.toString() else monday.dayMonth()
    return "$start – ${end.dayMonth()}"
}

/** «5 дней», «1 день», «22 дня». */
internal fun days(count: Int): String {
    val word = when {
        count % 100 in 11..14 -> "дней"
        count % 10 == 1 -> "день"
        count % 10 in 2..4 -> "дня"
        else -> "дней"
    }
    return "$count $word"
}

private fun Attendance.color() = when (this) {
    Attendance.PRESENT -> Green
    Attendance.EXCUSED -> Warning
    Attendance.ABSENT -> Danger
    Attendance.UNKNOWN -> Color.Gray
}

/**
 * «Главная»: посещаемость, стрик и успеваемость. Всё это бэкенд отдаёт только с токеном,
 * поэтому без входа экран показывает приглашение войти, а не пустые карточки.
 */
@Composable
fun HomeScreen(onLogin: () -> Unit, viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val accent = MaterialTheme.colorScheme.primary
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind { drawAmbientGlow(accent, darkTheme) }
            .dotGrid(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Главная",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp),
            )

            when {
                state.isBootstrapping -> Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 120.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = accent) }

                !state.isAuthenticated -> SignInInvite(onLogin = onLogin)

                else -> Authorized(state = state, viewModel = viewModel)
            }

            Spacer(Modifier.height(navBarInset()))
        }
    }

    state.openSubject?.let { subject ->
        ScoresSheet(
            subject = subject,
            lessons = state.scores,
            isLoading = state.scoresLoading,
            error = state.scoresError,
            onDismiss = viewModel::closeSubject,
        )
    }
}

/** Без токена бэкенд не отдаёт ничего из этого экрана — честно объясняем и зовём войти. */
@Composable
private fun SignInInvite(onLogin: () -> Unit) {
    Column(modifier = Modifier.padding(top = 40.dp)) {
        Text(
            text = "Посещаемость и баллы",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Стрик посещений, отметки по парам и баллы по предметам колледж отдаёт " +
                "только вошедшим. Логин и пароль — те же, что в личном кабинете.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
        )
        ShinyPill(text = "Войти", onClick = onLogin)
    }
}

@Composable
private fun Authorized(state: HomeUiState, viewModel: HomeViewModel) {
    val user = state.user

    Text(
        text = user?.username.orEmpty(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )

    Spacer(Modifier.height(20.dp))

    StreakCard(state)

    Spacer(Modifier.height(20.dp))

    WeekNav(
        title = weekRange(state.weekStart),
        onPrev = { viewModel.shiftWeek(-1) },
        onNext = { viewModel.shiftWeek(1) },
        onToday = viewModel::goToCurrentWeek,
    )

    Spacer(Modifier.height(16.dp))

    when {
        state.isLoading && state.records.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

        state.error != null -> ErrorBlock(state.error ?: "", onRetry = viewModel::refresh)

        state.records.isEmpty() -> Text(
            text = "За эту неделю отметок нет",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp),
        )

        else -> {
            StatsRow(state)
            Spacer(Modifier.height(16.dp))
            state.records.groupBy { it.date }.toSortedMap().forEach { (date, records) ->
                DayBlock(date, records)
            }
        }
    }

    if (state.subjects.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Успеваемость",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        state.subjects.forEach { subject ->
            SubjectRow(subject) { viewModel.openSubject(subject) }
        }
    }
}

/** Стрик: главное число крупно, остальное — подписью, как в шапке расписания. */
@Composable
private fun StreakCard(state: HomeUiState) {
    val streak = state.streak
    Card(
        modifier = Modifier.fillMaxWidth().hairline(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Стрик посещений",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (streak == null) "—" else days(streak.current),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (streak != null) {
                Text(
                    text = "Лучший — ${days(streak.longest)} · посещаемость ${streak.rate.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${streak.daysAttended} из ${streak.schoolDays} учебных дней" +
                        (streak.lastAttended?.let { ", последний — ${it.dayMonth()}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StatsRow(state: HomeUiState) {
    val stats = state.stats
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("Был", stats.present.toString(), Green, Modifier.weight(1f))
        Stat("Ув.", stats.excused.toString(), Warning, Modifier.weight(1f))
        Stat("Н/У", stats.absent.toString(), Danger, Modifier.weight(1f))
        Stat("Всего", "${stats.percent}%", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .hairline(RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayBlock(date: LocalDate, records: List<AttendanceRecord>) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = date.dayTitle(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        records.forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .hairline(RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(record.attendance.color()),
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            record.start?.format(TIME)?.let { "$it – ${record.end?.format(TIME) ?: ""}" },
                            record.room.takeIf { it.isNotBlank() },
                            record.attendance.title,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectRow(subject: Subject, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .hairline(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = subject.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun WeekNav(title: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Round(R.drawable.ic_chevron_left, "Предыдущая неделя", onPrev)
        Spacer(Modifier.width(8.dp))
        Round(R.drawable.ic_calendar, "Текущая неделя", onToday)
        Spacer(Modifier.width(8.dp))
        Round(R.drawable.ic_chevron_right, "Следующая неделя", onNext)
    }
}

@Composable
private fun Round(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ErrorBlock(text: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Повторить")
        }
    }
}
