package ru.dzhaparidze.mykct.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.dzhaparidze.mykct.data.api.AttendanceRecord
import ru.dzhaparidze.mykct.data.api.AttendanceStats
import ru.dzhaparidze.mykct.data.api.CollegeApi
import ru.dzhaparidze.mykct.data.api.Leaderboard
import ru.dzhaparidze.mykct.data.api.Motivation
import ru.dzhaparidze.mykct.data.net.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import ru.dzhaparidze.mykct.data.api.Streak
import ru.dzhaparidze.mykct.data.api.Subject
import ru.dzhaparidze.mykct.data.api.SubjectLesson
import ru.dzhaparidze.mykct.data.auth.AuthService
import ru.dzhaparidze.mykct.data.auth.User
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Рейтинг грузится отдельно и только по открытию вкладки. */
sealed interface LeaderboardFeed {
    data object Idle : LeaderboardFeed
    data object Loading : LeaderboardFeed
    data class Loaded(val board: Leaderboard) : LeaderboardFeed
    /** 404 (выключен на сервере) или 403: показываем "недоступен", а не ошибку. */
    data object Unavailable : LeaderboardFeed
    data class Failed(val message: String) : LeaderboardFeed
}

data class HomeUiState(
    val user: User? = null,
    val isBootstrapping: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val records: List<AttendanceRecord> = emptyList(),
    val stats: AttendanceStats = AttendanceStats(0, 0, 0, 0),
    /** Строка под процентом: приходит позже самих цифр, до неё карточка живёт без неё. */
    val motivation: String? = null,
    val streak: Streak? = null,
    val leaderboard: LeaderboardFeed = LeaderboardFeed.Idle,
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Открытый предмет и его баллы: лист успеваемости грузится отдельно. */
    val openSubject: Subject? = null,
    val scores: List<SubjectLesson> = emptyList(),
    val scoresLoading: Boolean = false,
    val scoresError: String? = null,
) {
    val isAuthenticated: Boolean get() = user != null

    /** Рейтинг по курсу есть только у студента с группой. */
    val canSeeLeaderboard: Boolean get() = user?.isStudent == true && !user.academicGroup.isNullOrBlank()

    /** Отметки выбранного дня - календарь показывает месяц, список под ним один день. */
    val dayRecords: List<AttendanceRecord> get() = records.filter { it.date == selected }

    /**
     * Неделя для листа стрика: всегда текущая, а не та, что открыта в календаре.
     * Галочки за неделю имеют смысл только "сейчас", листать их некуда.
     */
    val weekStart: LocalDate
        get() = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

/**
 * "Главная": посещаемость за месяц, стрик и успеваемость - всё, что бэкенд отдаёт
 * только с токеном. Без входа экран показывает приглашение войти и ничего не грузит.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = AuthService.get(application)
    private val api = CollegeApi(auth)

    private val _state = MutableStateFlow(HomeUiState())
    private var leaderboardJob: Job? = null
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.state.collect { session ->
                val wasAuthenticated = _state.value.isAuthenticated
                _state.update { it.copy(user = session.user, isBootstrapping = session.isBootstrapping) }
                // Загружаемся один раз на переход «вошёл», а не на каждое обновление токена
                if (session.user != null && !wasAuthenticated) load()
                if (session.user == null) _state.update {
                    it.copy(
                        records = emptyList(),
                        streak = null,
                        leaderboard = LeaderboardFeed.Idle,
                        subjects = emptyList(),
                        stats = AttendanceStats(0, 0, 0, 0),
                    )
                }
            }
        }
    }

    fun refresh() = load()

    fun shiftMonth(months: Long) {
        _state.update { val month = it.month.plusMonths(months); it.copy(month = month, selected = month.pick()) }
        load()
    }

    /** "Сегодня": в текущем месяце только подсвечиваем день, из другого - возвращаемся и грузим. */
    fun goToToday() {
        val today = LocalDate.now()
        if (YearMonth.from(today) == _state.value.month) {
            _state.update { it.copy(selected = today) }
            return
        }
        _state.update { it.copy(month = YearMonth.from(today), selected = today) }
        load()
    }

    /** По открытию вкладки: уже загруженный или грузящийся рейтинг не трогаем. */
    fun loadLeaderboard() {
        val feed = _state.value.leaderboard
        if (feed is LeaderboardFeed.Loading || feed is LeaderboardFeed.Loaded) return
        reloadLeaderboard()
    }

    fun reloadLeaderboard() {
        if (!_state.value.canSeeLeaderboard) return
        leaderboardJob?.cancel()
        _state.update { it.copy(leaderboard = LeaderboardFeed.Loading) }
        leaderboardJob = viewModelScope.launch {
            val feed = try {
                api.leaderboard()?.let { LeaderboardFeed.Loaded(it) } ?: LeaderboardFeed.Failed(LEADERBOARD_ERROR)
            } catch (e: ApiException) {
                if (e.status == 403 || e.status == 404) LeaderboardFeed.Unavailable
                else LeaderboardFeed.Failed(e.message ?: LEADERBOARD_ERROR)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LeaderboardFeed.Failed(LEADERBOARD_ERROR)
            }
            _state.update { it.copy(leaderboard = feed) }
        }
    }

    fun selectDay(date: LocalDate) = _state.update { it.copy(selected = date) }

    fun openSubject(subject: Subject) {
        _state.update { it.copy(openSubject = subject, scores = emptyList(), scoresLoading = true, scoresError = null) }
        viewModelScope.launch {
            val (start, end) = semester()
            try {
                val scores = api.scores(subject.id, start, end)
                _state.update { it.copy(scores = scores, scoresLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(scoresLoading = false, scoresError = e.message ?: "Не удалось загрузить баллы") }
            }
        }
    }

    fun closeSubject() = _state.update { it.copy(openSubject = null, scores = emptyList(), scoresError = null) }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    private fun load() {
        if (!_state.value.isAuthenticated) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val month = _state.value.month
            try {
                val records = api.attendance(month.atDay(1), month.atEndOfMonth())
                val stats = AttendanceStats.of(records)
                _state.update { it.copy(records = records, stats = stats, motivation = null) }
                // Фраза грузится отдельно и молча: карточка уже нарисована цифрами,
                // а строка проявляется, когда придёт (или сразу, если она из заготовок).
                if (stats.total > 0) {
                    val line = Motivation.line(stats)
                    _state.update { it.copy(motivation = line) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Не удалось загрузить посещаемость") }
            }
            // Стрик и предметы не должны падать вместе с посещаемостью: у каждого свой блок
            runCatching { api.streak() }.onSuccess { streak -> _state.update { it.copy(streak = streak) } }
            runCatching { api.subjects() }.onSuccess { subjects -> _state.update { it.copy(subjects = subjects) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    /** Какой день открыть при перелистывании: в текущем месяце - сегодня, иначе первое число. */
    private fun YearMonth.pick(): LocalDate =
        if (this == YearMonth.now()) LocalDate.now() else atDay(1)

    /**
     * Полугодие как в iOS: январь–июнь и сентябрь–декабрь. Июль и август — каникулы,
     * их относим ко второму полугодию, иначе в списке баллов пусто без объяснений.
     */
    private fun semester(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return if (today.monthValue in 1..6) {
            LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 6, 30)
        } else {
            LocalDate.of(today.year, 9, 1) to LocalDate.of(today.year, 12, 31)
        }
    }
}

private const val LEADERBOARD_ERROR = "Не удалось загрузить рейтинг"
