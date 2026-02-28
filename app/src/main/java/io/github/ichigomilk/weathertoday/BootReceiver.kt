package io.github.ichigomilk.weathertoday

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidget::class.java))
            if (ids.isNotEmpty()) {
                val refreshIntent = Intent(context, WeatherWidget::class.java).apply {
                    action = ACTION_REFRESH
                }
                context.sendBroadcast(refreshIntent)
            }
        }
    }
}
