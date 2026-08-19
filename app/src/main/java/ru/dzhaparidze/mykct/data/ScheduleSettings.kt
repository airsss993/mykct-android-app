package ru.dzhaparidze.mykct.data

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Сколько дней подряд показывать. Значения и дефолт — из iOS (`DefaultScheduleView`),
 * чтобы у студента с двумя телефонами расписание выглядело одинаково.
 */
enum class ScheduleView(val title: String, val days: Int) {
    TODAY("Сегодня", 1),
    THREE_DAYS("3 дня", 3),
    WEEK("Неделя", 7),
}

/** Настройки расписания. В iOS дефолт — «3 дня» и выходные не скрыты. */
data class ScheduleSettings(
    val view: ScheduleView = ScheduleView.THREE_DAYS,
    val skipWeekends: Boolean = false,
)

/** Две строки в SharedPreferences — как тема и выбор группы. */
class ScheduleSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("schedule", Context.MODE_PRIVATE)

    fun load(): ScheduleSettings = ScheduleSettings(
        view = prefs.getString(VIEW, null)
            ?.let { name -> ScheduleView.entries.firstOrNull { it.name == name } }
            ?: ScheduleSettings().view,
        skipWeekends = prefs.getBoolean(SKIP_WEEKENDS, false),
    )

    fun save(settings: ScheduleSettings) {
        prefs.edit()
            .putString(VIEW, settings.view.name)
            .putBoolean(SKIP_WEEKENDS, settings.skipWeekends)
            .apply()
        changed.tryEmit(settings)
    }

    companion object {
        /**
         * Настройки правят на экране настроек, а применяет их ViewModel расписания —
         * она живёт дольше обоих экранов, поэтому изменения приезжают сюда
         * (тот же приём, что у [SelectionStore.changed]).
         */
        val changed = MutableSharedFlow<ScheduleSettings>(extraBufferCapacity = 1)

        private const val VIEW = "view"
        private const val SKIP_WEEKENDS = "skip_weekends"
    }
}
