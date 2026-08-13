package ru.dzhaparidze.mykct.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.MockScheduleRepository
import ru.dzhaparidze.mykct.data.ScheduleRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

data class DayCell(
    val date: LocalDate,
    val lessonCount: Int,
    val isToday: Boolean,
)

data class ScheduleUiState(
    val weekStart: LocalDate,
    val selectedDate: LocalDate,
    val days: List<DayCell> = emptyList(),
    val lessons: List<Lesson> = emptyList(),
    val passedCount: Int = 0,
    val isLoading: Boolean = true,
    val error: Boolean = false,
) {
    /** Доля прошедших пар выбранного дня — под кольцо прогресса из референса. */
    val progress: Float get() = if (lessons.isEmpty()) 0f else passedCount.toFloat() / lessons.size
}

class ScheduleViewModel(
    private val repository: ScheduleRepository = MockScheduleRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        ScheduleUiState(weekStart = mondayOf(today()), selectedDate = today()),
    )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private var weekLessons: List<Lesson> = emptyList()

    init {
        loadWeek()
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

    fun retry() = loadWeek()

    private fun loadWeek() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = false) }
            try {
                weekLessons = repository.weekSchedule(_state.value.weekStart)
                applyWeek()
            } catch (e: Exception) {
                weekLessons = emptyList()
                _state.update { it.copy(isLoading = false, error = true, days = emptyList(), lessons = emptyList()) }
            }
        }
    }

    private fun applyWeek() {
        val state = _state.value
        val today = today()
        val byDate = weekLessons.groupBy { it.date }

        val days = (0..6).map { offset ->
            val date = state.weekStart.plusDays(offset.toLong())
            DayCell(
                date = date,
                lessonCount = byDate[date].orEmpty().size,
                isToday = date == today,
            )
        }

        val lessons = byDate[state.selectedDate].orEmpty().sortedBy { it.start }
        val passed = when {
            state.selectedDate.isBefore(today) -> lessons.size
            state.selectedDate.isAfter(today) -> 0
            else -> LocalTime.now().let { now -> lessons.count { !it.end.isAfter(now) } }
        }

        _state.update {
            it.copy(days = days, lessons = lessons, passedCount = passed, isLoading = false, error = false)
        }
    }

    private fun today() = LocalDate.now()

    private fun mondayOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
