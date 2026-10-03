package ru.dzhaparidze.mykct.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import ru.dzhaparidze.mykct.data.api.AttendanceStats
import ru.dzhaparidze.mykct.data.api.AttendanceRecord
import ru.dzhaparidze.mykct.data.api.Subject
import ru.dzhaparidze.mykct.feature.navBarInset
import ru.dzhaparidze.mykct.feature.schedule.components.subjectIcon
import ru.dzhaparidze.mykct.ui.Fade
import ru.dzhaparidze.mykct.ui.HeroSummary
import ru.dzhaparidze.mykct.ui.Phase
import ru.dzhaparidze.mykct.ui.Swirl
import ru.dzhaparidze.mykct.ui.phaseOf
import ru.dzhaparidze.mykct.ui.PullToRefresh
import ru.dzhaparidze.mykct.ui.ScreenTitle
import ru.dzhaparidze.mykct.ui.SegmentedSwitch
import ru.dzhaparidze.mykct.ui.ShinyPill
import ru.dzhaparidze.mykct.ui.hairline
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import ru.dzhaparidze.mykct.ui.theme.GreenLime
import ru.dzhaparidze.mykct.ui.theme.statusDanger
import ru.dzhaparidze.mykct.ui.theme.statusGreen
import ru.dzhaparidze.mykct.ui.theme.statusWarning
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val RU = Locale.forLanguageTag("ru-RU")
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

private val MONTHS_GENITIVE = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

/** Разделы экрана: посещаемость и успеваемость показываются по очереди, а не подряд. */
private val TABS = listOf("Посещаемость", "Успеваемость")

internal fun LocalDate.dayMonth() = "$dayOfMonth ${MONTHS_GENITIVE[monthValue - 1]}"

private fun LocalDate.dayTitle() =
    "${dayOfWeek.getDisplayName(TextStyle.FULL, RU).replaceFirstChar { it.uppercase() }}, ${dayMonth()}"

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

/** «1 пара», «4 пары», «11 пар». */
private fun lessons(count: Int): String {
    val word = when {
        count % 100 in 11..14 -> "пар"
        count % 10 == 1 -> "пара"
        count % 10 in 2..4 -> "пары"
        else -> "пар"
    }
    return "$count $word"
}

/** «1 предмет», «3 предмета», «12 предметов». */
private fun subjects(count: Int): String {
    val word = when {
        count % 100 in 11..14 -> "предметов"
        count % 10 == 1 -> "предмет"
        count % 10 in 2..4 -> "предмета"
        else -> "предметов"
    }
    return "$count $word"
}

@Composable
private fun Attendance.color() = when (this) {
    Attendance.PRESENT -> statusGreen
    Attendance.EXCUSED -> statusWarning
    Attendance.ABSENT -> statusDanger
    Attendance.UNKNOWN -> Color.Gray
}

/**
 * «Главная»: посещаемость и успеваемость. Всё это бэкенд отдаёт только с токеном,
 * поэтому без входа экран показывает приглашение войти, а не пустые карточки.
 *
 * Верстка повторяет расписание: тот же фон со светом, тот же `ScreenTitle` с огоньком
 * стрика и те же карточки. Посещаемость показывает календарь месяца, а не кольцо с
 * процентом. Стрик отдельной карточкой не дублируется - он живёт в огоньке и его листе.
 */
@Composable
fun HomeScreen(onLogin: () -> Unit, viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var streakOpen by rememberSaveable { mutableStateOf(false) }

    val accent = MaterialTheme.colorScheme.primary

    // Фон, свет и сетка — в `AppShell`, общие на все экраны.
    Box(modifier = Modifier.fillMaxSize()) {
        // `isRefreshing` только поверх уже показанных данных: на первой загрузке
        // крутится свой индикатор в теле экрана, и два спиннера сразу читались бы
        // как подвисание.
        PullToRefresh(
            isRefreshing = state.isLoading &&
                (state.records.isNotEmpty() || state.subjects.isNotEmpty()),
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            ScreenTitle(text = "Главная") {
                // Огонёк — только у вошедшего: без токена стрика просто нет.
                if (state.streak != null) {
                    StreakFlame(onClick = { streakOpen = true })
                    Spacer(Modifier.width(4.dp))
                }
                state.user?.username?.let { UserPill(it) }
            }

            // Первый кадр экрана — индикатор поверх пустоты; он гаснет, и на его месте
            // проявляется либо приглашение войти, либо данные. Мгновенная подмена
            // читалась как мигание: автологин отвечает за доли секунды.
            Fade(target = state.isBootstrapping to state.isAuthenticated) { (loading, authorized) ->
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 120.dp),
                        contentAlignment = Alignment.Center,
                    ) { Swirl(color = accent) }

                    !authorized -> SignInInvite(onLogin = onLogin)

                    else -> Authorized(state = state, viewModel = viewModel)
                }
            }

            Spacer(Modifier.height(navBarInset()))
        }
        }
    }

    state.streak?.let { streak ->
        if (streakOpen) {
            StreakSheet(
                streak = streak,
                records = state.records,
                stats = state.stats,
                weekStart = state.weekStart,
                onDismiss = { streakOpen = false },
            )
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

/** Логин в шапке — той же пилюлей, что группа в расписании, чтобы строки совпали. */
@Composable
private fun UserPill(login: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .widthIn(max = 200.dp)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = login,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Spacer(Modifier.height(20.dp))

    SegmentedSwitch(items = TABS, selected = tab, onSelect = { tab = it })

    Spacer(Modifier.height(24.dp))

    // Разделы уезжают в ту сторону, куда переключили, — движение повторяет ход бегунка.
    // `SizeTransform(clip = false)`: у вкладок разная высота, и без него список
    // подрезается по высоте соседа на всё время перехода.
    AnimatedContent(
        targetState = tab,
        transitionSpec = {
            val dx = if (targetState > initialState) 1 else -1
            (slideInHorizontally(tween(220)) { dx * it / 6 } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(220)) { -dx * it / 6 } + fadeOut(tween(160))) using
                SizeTransform(clip = false)
        },
        label = "home-tab",
    ) { current ->
        Column(modifier = Modifier.fillMaxWidth()) {
            if (current == 0) AttendanceTab(state, viewModel) else PerformanceTab(state, viewModel)
        }
    }
}

/** Посещаемость за месяц: календарь, сводка под ним и пары выбранного дня. */
@Composable
private fun AttendanceTab(state: HomeUiState, viewModel: HomeViewModel) {
    val empty = state.records.isEmpty()

    AttendanceCalendar(
        month = state.month,
        selected = state.selected,
        records = state.records,
        onPrev = { viewModel.shiftMonth(-1) },
        onNext = { viewModel.shiftMonth(1) },
        onToday = viewModel::goToToday,
        onSelect = viewModel::selectDay,
    )

    Spacer(Modifier.height(14.dp))

    MonthSummary(
        stats = state.stats,
        month = state.month,
        text = when {
            state.isLoading && empty -> "Загружаем…"
            state.error != null -> "Нет данных"
            empty -> "Отметок за месяц нет"
            else -> "Был на ${state.stats.present} из ${lessons(state.stats.total)}"
        },
        hasData = !empty,
        motivation = state.motivation,
    )

    Spacer(Modifier.height(20.dp))

    Fade(
        target = phaseOf(
            isLoading = state.isLoading && empty,
            error = state.error,
            isEmpty = empty,
        ),
    ) { phase ->
        when (phase) {
            Phase.Loading -> Loading()

            Phase.Error -> ErrorBlock(state.error ?: "", onRetry = viewModel::refresh)

            Phase.Empty -> Empty("За этот месяц отметок нет")

            // Список - только выбранный день. Весь месяц простынёй никто не читает,
            // для "когда я пропустил" выше стоит календарь.
            Phase.Content -> DayBlock(state.selected, state.dayRecords)
        }
    }
}

/**
 * Сводка месяца: кольцо с процентом и строка "был на столько-то из стольких".
 * Кольцо здесь маленькое и стоит сбоку от текста - во всю ширину экрана оно и было
 * тем самым дашбордом, из-за которого пришлось переделывать вкладку.
 */
@Composable
private fun MonthSummary(
    stats: AttendanceStats,
    month: YearMonth,
    text: String,
    hasData: Boolean,
    motivation: String?,
) {
    // Края карточки уходят в акцент: плоская заливка рядом с градиентом в кольце
    // читается как вырезанный прямоугольник, а не как одна поверхность.
    val surface = MaterialTheme.colorScheme.surface
    val edge = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f).compositeOver(surface)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(edge, surface, edge)))
            .hairline(RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PercentRing(percent = stats.percent, hasData = hasData)

        Column(modifier = Modifier.weight(1f).padding(start = 18.dp)) {
            // Фраза приходит позже цифр, и до неё сверху стоит название месяца:
            // так строка не пустует и карточка не подпрыгивает, когда фраза придёт.
            Text(
                text = motivation
                    ?: "Посещаемость за ${MONTHS_NOMINATIVE[month.monthValue - 1].lowercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Доля посещённых пар кольцом: дуга растёт от двенадцати часов, число стоит внутри. */
@Composable
private fun PercentRing(percent: Int, hasData: Boolean) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    // Дуга залита градиентом, а не одним цветом: лайм сверху, зелёный снизу.
    val arc = Brush.linearGradient(listOf(GreenLime, statusGreen))
    val sweep by animateFloatAsState(
        targetValue = if (hasData) percent / 100f else 0f,
        animationSpec = tween(700),
        label = "percent",
    )

    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (sweep > 0f) {
                drawArc(
                    brush = arc,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = if (hasData) "$percent%" else "-",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Успеваемость: предметы полугодия, баллы по каждому — в листе по нажатию. */
@Composable
private fun PerformanceTab(state: HomeUiState, viewModel: HomeViewModel) {
    val empty = state.subjects.isEmpty()

    HeroSummary(
        caption = "Текущее полугодие",
        value = when {
            state.isLoading && empty -> "Загружаем…"
            empty -> "Нет данных"
            else -> subjects(state.subjects.size)
        },
        subtitle = "Нажми на предмет — покажем баллы по занятиям",
    )

    Spacer(Modifier.height(24.dp))

    Fade(
        target = phaseOf(isLoading = state.isLoading && empty, error = null, isEmpty = empty),
    ) { phase ->
        when (phase) {
            Phase.Loading -> Loading()
            Phase.Empty -> Empty("Колледж не отдал ни одного предмета")
            // Ошибку успеваемости показывает вкладка посещаемости: `error` в состоянии один.
            else -> state.subjects.forEach { subject ->
                SubjectRow(subject) { viewModel.openSubject(subject) }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) { Swirl() }
}

@Composable
private fun Empty(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

/** Короткая метка для пилюли: полное «Не был (Н/У)» в строку не помещается. */
private val Attendance.short: String
    get() = when (this) {
        Attendance.PRESENT -> "Был"
        Attendance.EXCUSED -> "Ув."
        Attendance.ABSENT -> "Н/У"
        Attendance.UNKNOWN -> "—"
    }

/**
 * День посещаемости — одна карточка со строками, а не стопка отдельных плашек:
 * у четырёх пар подряд рамки и скругления превращались в рябь. Статус показывают
 * иконка предмета в тонированном круге и пилюля справа — цветной точки для этого
 * мало, она читается как маркер списка, а не как оценка присутствия.
 */
@Composable
private fun DayBlock(date: LocalDate, records: List<AttendanceRecord>) {
    if (records.isEmpty()) {
        Empty("${date.dayTitle()} - пар не было")
        return
    }
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date.dayTitle(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${records.count { it.attendance == Attendance.PRESENT }} из ${records.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .hairline(RoundedCornerShape(20.dp)),
        ) {
            records.forEachIndexed { index, record ->
                // Разделитель начинается после иконки, иначе строки не читаются списком.
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 64.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    )
                }
                RecordRow(record)
            }
        }
    }
}

@Composable
private fun RecordRow(record: AttendanceRecord) {
    val color = record.attendance.color()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(subjectIcon(record.title)),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }
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
                    record.start?.let { "${it.format(TIME)} – ${record.end?.format(TIME) ?: ""}" },
                    record.room.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = record.attendance.short,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** Иконка предмета — та же, что водяным знаком на карточке пары в расписании. */
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(subjectIcon(subject.title)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = subject.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
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
