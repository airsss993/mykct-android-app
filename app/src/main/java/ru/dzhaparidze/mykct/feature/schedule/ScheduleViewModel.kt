package ru.dzhaparidze.mykct.feature.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.ApiScheduleRepository
import ru.dzhaparidze.mykct.data.ScheduleRepository
import ru.dzhaparidze.mykct.data.ScheduleSettings
import ru.dzhaparidze.mykct.data.ScheduleSettingsStore
import ru.dzhaparidze.mykct.data.ScheduleView
import ru.dzhaparidze.mykct.data.Selection
import ru.dzhaparidze.mykct.data.SelectionStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DayCell(
    val date: LocalDate,
    val lessonCount: Int,
    val isToday: Boolean,
)

/** Открытая пара и её детали с портала: грузятся по нажатию, а не вместе с неделей. */
data class LessonDetails(
    val lesson: Lesson,
    val rows: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** Один день диапазона: заголовок + его пары. */
data class DaySchedule(
    val date: LocalDate,
    val lessons: List<Lesson>,
)

data class ScheduleUiState(
    val weekStart: LocalDate,
    val selectedDate: LocalDate,
    val selection: Selection,
    val settings: ScheduleSettings = ScheduleSettings(),
    val days: List<DayCell> = emptyList(),
    /** Дни, которые сейчас показаны: один, три или вся неделя — по [ScheduleSettings.view]. */
    val visible: List<DaySchedule> = emptyList(),
    val isLoading: Boolean = true,
    /** Текст ошибки от сети; null — всё в порядке. */
    val error: String? = null,
    /** null — лист с парой закрыт. */
    val details: LessonDetails? = null,
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SelectionStore(application)
    private val settingsStore = ScheduleSettingsStore(application)
    private val repository: ScheduleRepository = ApiScheduleRepository()

    private val _state = MutableStateFlow(
        ScheduleUiState(
            weekStart = mondayOf(today()),
            selectedDate = today(),
            selection = store.load(),
            settings = settingsStore.load(),
        ),
    )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private var weekLessons: List<Lesson> = emptyList()

    init {
        loadWeek()
        // Выбор могли поменять в настройках («использовать мою группу») — подхватываем
        viewModelScope.launch {
            SelectionStore.changed.collect { selection ->
                if (selection == _state.value.selection) return@collect
                _state.update { it.copy(selection = selection) }
                loadWeek()
            }
        }
        // Вид и «пропускать выходные» — чистый пересчёт: неделя уже в weekLessons
        viewModelScope.launch {
            ScheduleSettingsStore.changed.collect { settings ->
                if (settings == _state.value.settings) return@collect
                _state.update { it.copy(settings = settings) }
                applyWeek()
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        applyWeek()
    }

    fun shiftWeek(weeks: Long) {
        val weekStart = _state.value.weekStart.plusWeeks(weeks)
        _state.update { it.copy(weekStart = weekStart, selectedDate = weekStart) }
        loadWeek()
    }

    fun goToToday() {
        val today = today()
        val weekStart = mondayOf(today)
        if (weekStart == _state.value.weekStart) {
            selectDate(today)
        } else {
            _state.update { it.copy(weekStart = weekStart, selectedDate = today) }
            loadWeek()
        }
    }

    fun updateSelection(selection: Selection) {
        if (selection == _state.value.selection) return
        _state.update { it.copy(selection = selection) }
        store.save(selection)
        loadWeek()
    }

    fun retry() = loadWeek()

    fun openLesson(lesson: Lesson) {
        _state.update { it.copy(details = LessonDetails(lesson)) }
        viewModelScope.launch {
            val result = runCatching { repository.classDetails(lesson.id) }
            _state.update { state ->
                // пока грузили, могли закрыть лист или открыть другую пару — ответ уже не нужен
                if (state.details?.lesson?.id != lesson.id) return@update state
                state.copy(
                    details = state.details.copy(
                        rows = result.getOrDefault(emptyList()),
                        isLoading = false,
                        error = result.exceptionOrNull()?.message,
                    ),
                )
            }
        }
    }

    fun closeLesson() = _state.update { it.copy(details = null) }

    private fun loadWeek() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                weekLessons = repository.weekSchedule(_state.value.weekStart, _state.value.selection)
                applyWeek()
            } catch (e: Exception) {
                weekLessons = emptyList()
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось загрузить расписание",
                        days = emptyList(),
                        visible = emptyList(),
                    )
                }
            }
        }
    }

    private fun applyWeek() {
        val state = _state.value
        val today = today()
        val byDate = weekLessons.groupBy { it.date }

        val dates = weekDays(state.weekStart, state.settings)

        val days = dates.map { date ->
            DayCell(
                date = date,
                lessonCount = byDate[date].orEmpty().size,
                isToday = date == today,
            )
        }

        val visible = visibleDays(dates, state.selectedDate, state.settings).map { date ->
            DaySchedule(date = date, lessons = byDate[date].orEmpty().sortedBy { it.start })
        }

        _state.update {
            it.copy(days = days, visible = visible, isLoading = false, error = null)
        }
    }

    private fun today() = LocalDate.now()

    private fun mondayOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

/** Дни полоски недели: пн–вс или пн–пт, если выходные скрыты. */
internal fun weekDays(weekStart: LocalDate, settings: ScheduleSettings): List<LocalDate> =
    (0..6).map { weekStart.plusDays(it.toLong()) }
        .filter { !settings.skipWeekends || it.dayOfWeek.value <= 5 }

/**
 * Какие дни рисует таймлайн. Диапазон никогда не вылезает за неделю: дальше данных
 * всё равно нет, их грузит `loadWeek()` целиком неделями. Выходные выброшены ещё
 * в [weekDays], так что «3 дня» со скрытыми выходными — это три рабочих дня, как в iOS.
 */
internal fun visibleDays(
    days: List<LocalDate>,
    selected: LocalDate,
    settings: ScheduleSettings,
): List<LocalDate> = when (settings.view) {
    ScheduleView.WEEK -> days
    else -> days.dropWhile { it < selected }.take(settings.view.days)
    // выбран выходной, а они скрыты — показываем хвост недели, а не пустоту
}.ifEmpty { days.takeLast(settings.view.days) }
