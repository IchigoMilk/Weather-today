package io.github.ichigomilk.weathertoday

import android.util.Log
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val ACTION_REFRESH = "io.github.ichigomilk.weathertoday.REFRESH"

class WeatherWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
        fetchAndUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WeatherWidget::class.java)
            )
            fetchAndUpdate(context, manager, ids)
        }
    }

    private fun fetchAndUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = Prefs(context)
                val repo = WeatherRepository()
                val result = repo.fetch(
                    prefs.latitude,
                    prefs.longitude,
                    prefs.timezone,
                    prefs.locationName
                )
                prefs.cachedJson = Gson().toJson(result)
                for (id in ids) {
                    manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
                    updateWidget(context, manager, id)
                }
            } catch (e: Exception) {
                Log.e("WeatherWidget", "Failed to fetch weather data", e)
            }
        }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val prefs = Prefs(context)
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val cachedJson = prefs.cachedJson
            val result = if (cachedJson.isNotBlank()) {
                try { Gson().fromJson(cachedJson, WeatherResult::class.java) } catch (_: Exception) { null }
            } else null

            views.setTextViewText(R.id.widget_location, result?.locationName ?: "-- WEATHER-TODAY --")
            views.setTextViewText(
                R.id.widget_updated,
                if (result != null) "UPDATED: ${result.updatedAt}" else "-- NO DATA --"
            )

            val serviceIntent = Intent(context, WeatherWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val refreshIntent = Intent(context, WeatherWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, pendingIntent)

            val settingsIntent = Intent(context, SettingsActivity::class.java)
            val settingsPi = android.app.PendingIntent.getActivity(
                context, 1, settingsIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_location, settingsPi)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
