package io.github.ichigomilk.weathertoday

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "weather_today_prefs"
private const val KEY_LATITUDE = "latitude"
private const val KEY_LONGITUDE = "longitude"
private const val KEY_TIMEZONE = "timezone"
private const val KEY_LOCATION_NAME = "location_name"
private const val KEY_CACHED_JSON = "cached_json"

private const val DEFAULT_LATITUDE = "35.6895"
private const val DEFAULT_LONGITUDE = "139.6917"
private const val DEFAULT_TIMEZONE = "Asia/Tokyo"
private const val DEFAULT_LOCATION_NAME = "Tokyo"

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var latitude: String
        get() = prefs.getString(KEY_LATITUDE, DEFAULT_LATITUDE) ?: DEFAULT_LATITUDE
        set(v) = prefs.edit { putString(KEY_LATITUDE, v) }

    var longitude: String
        get() = prefs.getString(KEY_LONGITUDE, DEFAULT_LONGITUDE) ?: DEFAULT_LONGITUDE
        set(v) = prefs.edit { putString(KEY_LONGITUDE, v) }

    var timezone: String
        get() = prefs.getString(KEY_TIMEZONE, DEFAULT_TIMEZONE) ?: DEFAULT_TIMEZONE
        set(v) = prefs.edit { putString(KEY_TIMEZONE, v) }

    var locationName: String
        get() = prefs.getString(KEY_LOCATION_NAME, DEFAULT_LOCATION_NAME) ?: DEFAULT_LOCATION_NAME
        set(v) = prefs.edit { putString(KEY_LOCATION_NAME, v) }

    var cachedJson: String
        get() = prefs.getString(KEY_CACHED_JSON, "") ?: ""
        set(v) = prefs.edit { putString(KEY_CACHED_JSON, v) }
}
