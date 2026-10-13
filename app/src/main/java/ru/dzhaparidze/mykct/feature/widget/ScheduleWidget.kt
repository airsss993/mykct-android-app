package ru.dzhaparidze.mykct.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.dzhaparidze.mykct.MainActivity
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.data.ApiScheduleRepository
import ru.dzhaparidze.mykct.data.Lesson
import ru.dzhaparidze.mykct.data.SelectionStore
import ru.dzhaparidze.mykct.data.slotCount
import ru.dzhaparidze.mykct.data.splitOwn
import ru.dzhaparidze.mykct.feature.schedule.components.subjectIcon
import ru.dzhaparidze.mykct.feature.schedule.dayMonth
import ru.dzhaparidze.mykct.feature.schedule.lessonsCount
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Высота строки из widget_row.xml и всего, что над списком: отступы, шапка, карточка. */
private const val ROW_DP = 36
private const val CHROME_DP = 160

/**
 * Виджет расписания - возвращён из iOS 1.x (MediumScheduleWidgetView): идущая или
 * ближайшая пара карточкой в фирменном градиенте, под ней следующие пары, сколько
 * влезает по высоте. Пары на сегодня кончились - показывает завтра.
 *
 * RemoteViews, а не Glance: ради одного виджета не тянуть библиотеку. Шрифт системный:
 * лаунчер не грузит шрифты из ресурсов приложения, Onest в разметке он молча игнорирует. Своих часов у
 * виджета нет, поэтому на ближайшую границу пары ставится будильник, а пары кешируются:
 * без сети виджет рисует то, что видел последним.
 */
class ScheduleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        update(context)

    override fun onDisabled(context: Context) =
        context.getSystemService(AlarmManager::class.java).cancel(updateIntent(context))

    private fun update(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Перерисовать со свежими данными: сменили группу, пришёл пуш об изменениях. */
        fun refresh(context: Context) {
            if (ids(context).isNotEmpty()) updateIntent(context).send()
        }
    }
}

private fun ids(context: Context): IntArray =
    AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ScheduleWidget::class.java))

private fun updateIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context,
    0,
    Intent(context, ScheduleWidget::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids(context)),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

private suspend fun render(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = ids(context)
    if (ids.isEmpty()) return
    val now = LocalDateTime.now()
    val day = load(context, now.toLocalDate())?.let { pickDay(it, now) }
    ids.forEach { id ->
        val height = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        manager.updateAppWidget(id, views(context, day, now.toLocalTime(), height))
    }
    // не загрузилось - пробуем снова через полчаса, а не на границе пары
    val next = day?.nextChange(now) ?: now.plusMinutes(30)
    // неточный будильник: точный требует разрешения, а опоздание на минуту виджету не страшно
    context.getSystemService(AlarmManager::class.java).set(
        AlarmManager.RTC,
        next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updateIntent(context),
    )
}

/** Пары на сегодня и завтра: из сети, без неё - из кеша. null - нет ни того, ни другого. */
private suspend fun load(context: Context, today: LocalDate): List<Lesson>? {
    val days = listOf(today, today.plusDays(1))
    val fresh = withTimeoutOrNull(8_000) {
        runCatching {
            val selection = SelectionStore(context).load()
            val repository = ApiScheduleRepository()
            // в воскресенье завтра - уже следующая неделя
            days.map { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }.distinct()
                .flatMap { repository.weekSchedule(it, selection).lessons.splitOwn(selection) }
                .filter { it.date in days }
        }.getOrNull()
    }
    val prefs = context.getSharedPreferences("widget", Context.MODE_PRIVATE)
    if (fresh != null) {
        val cached = Cached(today.toString(), fresh.map(CachedLesson::of))
        prefs.edit().putString(CACHE, Json.encodeToString(cached)).apply()
        return fresh
    }
    val cached = prefs.getString(CACHE, null)
        ?.let { runCatching { Json.decodeFromString<Cached>(it) }.getOrNull() }
        // вчерашний кеш ещё знает сегодняшние пары, позавчерашний - уже нет
        ?.takeIf { LocalDate.parse(it.savedOn) >= today.minusDays(1) }
        ?: return null
    return cached.lessons.map { it.toLesson() }.filter { it.date >= today }
}

private const val CACHE = "lessons"

@Serializable
private class Cached(val savedOn: String, val lessons: List<CachedLesson>)

/** Подгруппы не храним: виджету от них нужен только склеенный заголовок. */
@Serializable
private class CachedLesson(
    val id: String,
    val date: String,
    val start: String,
    val end: String,
    val title: String,
    val room: String,
) {
    fun toLesson() = Lesson(
        id = id,
        date = LocalDate.parse(date),
        start = LocalTime.parse(start),
        end = LocalTime.parse(end),
        title = title,
        topic = "",
        room = room,
        colorHex = null,
    )

    companion object {
        fun of(lesson: Lesson) = CachedLesson(
            lesson.id, lesson.date.toString(), lesson.start.toString(), lesson.end.toString(),
            lesson.displayTitle, lesson.room,
        )
    }
}

/** Что показывает виджет: [lessons] - пары дня, начиная с идущей или ближайшей. */
internal data class WidgetDay(
    val date: LocalDate,
    val isToday: Boolean,
    /** Пар за день по звонкам, как в полоске недели. */
    val slots: Int,
    val lessons: List<Lesson>,
)

/** Оставшиеся пары сегодня, а если их нет - весь завтрашний день. */
internal fun pickDay(lessons: List<Lesson>, now: LocalDateTime): WidgetDay {
    val today = now.toLocalDate()
    fun of(date: LocalDate) = lessons.filter { it.date == date }.sortedBy { it.start }
    val todays = of(today)
    val left = todays.filter { it.end > now.toLocalTime() }
    if (left.isNotEmpty()) return WidgetDay(today, true, todays.slotCount(), left)
    val tomorrow = of(today.plusDays(1))
    return WidgetDay(today.plusDays(1), false, tomorrow.slotCount(), tomorrow)
}

/** Когда картинка устареет: ближайшее начало или конец пары, а для завтрашнего дня - полночь. */
internal fun WidgetDay.nextChange(now: LocalDateTime): LocalDateTime =
    if (isToday) {
        date.atTime(lessons.flatMap { listOf(it.start, it.end) }.filter { it > now.toLocalTime() }.min())
    } else {
        date.atStartOfDay()
    }

private fun views(context: Context, day: WidgetDay?, now: LocalTime, heightDp: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_schedule)
    views.setOnClickPendingIntent(
        R.id.widget_root,
        PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ),
    )

    if (day == null) {
        views.setTextViewText(R.id.widget_day, "Расписание")
        views.setTextViewText(R.id.widget_count, "")
        return views.message(R.drawable.ic_refresh, "Не удалось загрузить", "Нажми, чтобы открыть приложение")
    }

    views.setTextViewText(R.id.widget_day, (if (day.isToday) "Сегодня, " else "Завтра, ") + day.date.dayMonth())
    views.setTextViewText(R.id.widget_count, if (day.slots > 0) lessonsCount(day.slots) else "")

    val first = day.lessons.firstOrNull()
        ?: return views.message(R.drawable.ic_check, "Пар нет", "Расписание обновится само")

    val isNow = day.isToday && first.start <= now
    views.setTextViewText(R.id.widget_card_time, "${first.start.format(TIME)} - ${first.end.format(TIME)}")
    views.setTextViewText(R.id.widget_card_title, first.displayTitle)
    views.setImageViewResource(R.id.widget_card_icon, subjectIcon(first.title))
    val meta = listOfNotNull(
        first.room.ifBlank { null },
        if (isNow) "идёт до ${first.end.format(TIME)}" else null,
    ).joinToString(" · ")
    views.setTextViewText(R.id.widget_card_meta, meta)
    views.setViewVisibility(R.id.widget_card_meta, if (meta.isEmpty()) View.GONE else View.VISIBLE)

    // 0 - лаунчер ещё не сообщил размер: одна строка влезает в любой допустимый
    val rows = if (heightDp == 0) 1 else ((heightDp - CHROME_DP) / ROW_DP).coerceAtLeast(0)
    day.lessons.drop(1).take(rows).forEach { lesson ->
        val row = RemoteViews(context.packageName, R.layout.widget_row)
        row.setTextViewText(R.id.row_time, lesson.start.format(TIME))
        row.setTextViewText(R.id.row_title, lesson.displayTitle)
        row.setTextViewText(R.id.row_room, lesson.room)
        views.addView(R.id.widget_list, row)
    }
    return views
}

/** Вместо карточки и списка - значок и две строки по центру. */
private fun RemoteViews.message(icon: Int, title: String, text: String) = apply {
    setViewVisibility(R.id.widget_card, View.GONE)
    setViewVisibility(R.id.widget_list, View.GONE)
    setViewVisibility(R.id.widget_message, View.VISIBLE)
    setImageViewResource(R.id.widget_message_icon, icon)
    setTextViewText(R.id.widget_message_title, title)
    setTextViewText(R.id.widget_message_text, text)
}
