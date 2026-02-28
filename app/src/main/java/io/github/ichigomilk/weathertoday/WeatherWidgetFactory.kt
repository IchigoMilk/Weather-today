package io.github.ichigomilk.weathertoday

import android.util.Log
import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson

class WeatherWidgetFactory(
    private val context: Context,
    private val widgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private val rows = mutableListOf<WidgetRow>()

    sealed class WidgetRow {
        data class Header(val label: String) : WidgetRow()
        data class ColHeader(val text: String) : WidgetRow()
        data class Entry(val entry: HourlyEntry) : WidgetRow()
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        rows.clear()
        val prefs = Prefs(context)
        val json = prefs.cachedJson
        if (json.isBlank()) return
        val result = try {
            Gson().fromJson(json, WeatherResult::class.java)
        } catch (e: Exception) {
            Log.e("WeatherWidgetFactory", "Failed to parse cached weather JSON", e)
            return
        }
        if (result.today.isNotEmpty()) {
            rows.add(WidgetRow.Header("-- TODAY --"))
            rows.add(WidgetRow.ColHeader(colHeaderText()))
            result.today.forEach { rows.add(WidgetRow.Entry(it)) }
        }
        if (result.tomorrow.isNotEmpty()) {
            rows.add(WidgetRow.Header("-- TOMORROW --"))
            rows.add(WidgetRow.ColHeader(colHeaderText()))
            result.tomorrow.forEach { rows.add(WidgetRow.Entry(it)) }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        return when (val row = rows[position]) {
            is WidgetRow.Header -> {
                val rv = RemoteViews(context.packageName, R.layout.widget_row_header)
                rv.setTextViewText(R.id.row_header_text, row.label)
                rv
            }
            is WidgetRow.ColHeader -> {
                val rv = RemoteViews(context.packageName, R.layout.widget_row_colheader)
                rv.setTextViewText(R.id.row_colheader_text, row.text)
                rv
            }
            is WidgetRow.Entry -> {
                val rv = RemoteViews(context.packageName, R.layout.widget_row)
                rv.setTextViewText(R.id.row_time, row.entry.time)
                rv.setTextViewText(R.id.row_temp, fmtTemp(row.entry.temperature))
                rv.setTextViewText(R.id.row_humidity, fmtPct(row.entry.humidity))
                rv.setTextViewText(R.id.row_precip_prob, fmtPct(row.entry.precipProb))
                rv.setTextViewText(R.id.row_precip, fmtMm(row.entry.precip))
                rv.setTextViewText(R.id.row_wind, fmtKmh(row.entry.windSpeed))
                rv.setTextViewText(R.id.row_pollen, fmtPollen(row.entry.pollen))
                rv.setTextViewText(R.id.row_cosmic, fmtCosmic(row.entry.cosmicRay))
                rv
            }
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 3

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    private fun colHeaderText() =
        "TIME  TEMP   HUM  P%   PREC   WIND   POLLEN  COSMIC"

    private fun fmtTemp(v: Double) = if (v.isNaN()) " --- " else "%+.1fC".format(v)
    private fun fmtPct(v: Int) = if (v < 0) " ---" else "%3d%%".format(v)
    private fun fmtMm(v: Double) = if (v.isNaN()) " --- " else "%.1fmm".format(v)
    private fun fmtKmh(v: Double) = if (v.isNaN()) " --- " else "%.1fkm/h".format(v)
    private fun fmtPollen(v: Double) = if (v.isNaN()) " ---" else "%.0f".format(v)
    private fun fmtCosmic(v: Double) = if (v.isNaN()) " ---" else "%.0f".format(v)
}
