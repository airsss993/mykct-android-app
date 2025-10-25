package ru.dzhaparidze.collegeapp.features.schedule.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.dzhaparidze.collegeapp.features.schedule.data.ScheduleRepositoryInterface
import ru.dzhaparidze.collegeapp.features.schedule.models.ScheduleEvent
import ru.dzhaparidze.collegeapp.features.schedule.utils.GroupsCatalog
import ru.dzhaparidze.collegeapp.features.schedule.views.TimePeriod
import java.text.SimpleDateFormat
import java.util.*

class ScheduleViewModel(private val repository: ScheduleRepositoryInterface) : ViewModel() {
    private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    val events = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private fun getLatestGroup(): String {
        val availableGroups = GroupsCatalog.allGroups
        return availableGroups.filter { it.contains("ИТ") }.maxBy { group ->
            val year = group.substringAfter("ИТ").substringBefore("-")
            year.toIntOrNull() ?: 0
        }
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun getDatePlusDays(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 2)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun getWeekStart(): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        } else {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun getWeekEnd(): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        } else {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    var selectedGroup by mutableStateOf(getLatestGroup())
    var selectedSubgroup by mutableStateOf("*")
    var selectedEnglishGroup by mutableStateOf("*")
    var selectedProfileSubgroup by mutableStateOf("*")
    var selectedStartDate by mutableStateOf(getCurrentDate())
    var selectedEndDate by mutableStateOf(getDatePlusDays())
    var selectedWeekOffset by mutableIntStateOf(0)

    fun loadSchedule() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val scheduleEvents = repository.getSchedule(
                    group = selectedGroup,
                    subgroup = selectedSubgroup,
                    englishGroup = selectedEnglishGroup,
                    profileSubgroup = selectedProfileSubgroup,
                    start = selectedStartDate,
                    end = selectedEndDate
                )
                _events.value = scheduleEvents
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "failed to load schedule"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSchedule(
        group: String? = null,
        subgroup: String? = null,
        englishGroup: String? = null,
        profileSubgroup: String? = null,
        timePeriod: TimePeriod? = null,
        startDate: String? = null,
        endDate: String? = null
    ) {
        group?.let { selectedGroup = it }
        subgroup?.let { selectedSubgroup = it }
        englishGroup?.let { selectedEnglishGroup = it }
        profileSubgroup?.let { selectedProfileSubgroup = it }

        timePeriod?.let { period ->
            when (period) {
                TimePeriod.TODAY -> {
                    val today = getCurrentDate()
                    selectedStartDate = today
                    selectedEndDate = today
                }

                TimePeriod.THREE_DAYS -> {
                    selectedStartDate = getCurrentDate()
                    selectedEndDate = getDatePlusDays()
                }

                TimePeriod.WEEK -> {
                    selectedWeekOffset = 0
                    selectedStartDate = getWeekStart()
                    selectedEndDate = getWeekEnd()
                }
            }
        }

        startDate?.let { selectedStartDate = it }
        endDate?.let { selectedEndDate = it }

        loadSchedule()
    }
}