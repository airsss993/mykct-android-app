package ru.dzhaparidze.collegeapp.features.schedule.data

import android.util.Log
import io.ktor.http.HttpMethod
import ru.dzhaparidze.collegeapp.BuildConfig
import ru.dzhaparidze.collegeapp.core.networking.Endpoint
import ru.dzhaparidze.collegeapp.core.networking.HttpClient.send
import ru.dzhaparidze.collegeapp.features.schedule.models.ScheduleResponse

interface ScheduleAPIInterface {
    suspend fun fetchSchedule(
        group: String,
        subgroup: String,
        englishGroup: String,
        profileSubgroup: String,
        start: String,
        end: String
    ): ScheduleResponse
}

class ScheduleAPI : ScheduleAPIInterface {
    override suspend fun fetchSchedule(
        group: String,
        subgroup: String,
        englishGroup: String,
        profileSubgroup: String,
        start: String,
        end: String
    ): ScheduleResponse {
        Log.d("ScheduleAPI", "Send request to get schedule")
        Log.d("ScheduleAPI", "Group: $group, Subgroup: $subgroup, English: $englishGroup, ProfileSubgroup: $profileSubgroup")
        Log.d("ScheduleAPI", "Range: $start - $end")

        val queryParams = mutableMapOf(
            "group" to group,
            "subgroup" to subgroup,
            "start" to start,
            "end" to end
        )

        if (englishGroup.isNotEmpty() && englishGroup != "*") {
            queryParams["english_group"] = englishGroup
        }

        if (profileSubgroup.isNotEmpty() && profileSubgroup != "*") {
            queryParams["profile_subgroup"] = profileSubgroup
        }

        val endpoint = Endpoint(
            path = "/api/v1/schedule",
            method = HttpMethod.Get,
            queryParams = queryParams,
        )

        val url = "${BuildConfig.API_BASE_URL}${endpoint.path}"
        Log.d("ScheduleAPI", "Request URL: $url")
        Log.d("ScheduleAPI", "Params: ${endpoint.queryParams}")

        return try {
            val baseUrl = BuildConfig.API_BASE_URL
            Log.d("ScheduleAPI", "Using base URL: $baseUrl")

            val response = send<ScheduleResponse>(endpoint, baseUrl)
            Log.d("ScheduleAPI", "Request success, get events: ${response.event.size}")
            response
        } catch (e: Exception) {
            Log.e("ScheduleAPI", "Error request: ${e.message}", e)
            throw e
        }
    }
}

