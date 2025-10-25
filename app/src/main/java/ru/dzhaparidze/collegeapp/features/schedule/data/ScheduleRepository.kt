package ru.dzhaparidze.collegeapp.features.schedule.data

import ru.dzhaparidze.collegeapp.features.schedule.models.ScheduleEvent

interface ScheduleRepositoryInterface {
    suspend fun getSchedule(
        group: String,
        subgroup: String,
        englishGroup: String,
        profileSubgroup: String,
        start: String,
        end: String
    ): List<ScheduleEvent>
}

class ScheduleRepository(private val api: ScheduleAPI) : ScheduleRepositoryInterface {
    override suspend fun getSchedule(
        group: String,
        subgroup: String,
        englishGroup: String,
        profileSubgroup: String,
        start: String,
        end: String
    ): List<ScheduleEvent> {
        val response = api.fetchSchedule(group, subgroup, englishGroup, profileSubgroup, start, end)
        val events = response.event.sortedWith { a, b ->
            if (a.day != b.day) {
                a.day.compareTo(b.day)
            } else {
                a.start.compareTo(b.start)
            }
        }
        return events
    }

}