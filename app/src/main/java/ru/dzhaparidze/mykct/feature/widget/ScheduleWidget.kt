package ru.dzhaparidze.mykct.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ru.dzhaparidze.mykct.MainActivity
import ru.dzhaparidze.mykct.R

/**
 * Заглушка "Скоро", как в iOS: место под виджет расписания уже занято, по нажатию
 * открывается приложение. Класс и ресурсы названы под будущий виджет, чтобы поставленная
 * заглушка сама превратилась в расписание после обновления.
 */
class ScheduleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_schedule)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        manager.updateAppWidget(ids, views)
    }
}
