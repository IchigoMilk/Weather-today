package io.github.ichigomilk.weathertoday

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService

class WeatherWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        return WeatherWidgetFactory(applicationContext, widgetId)
    }
}
