package io.github.ichigomilk.weathertoday

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.util.TimeZone

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        val etLocation = findViewById<EditText>(R.id.et_location_name)
        val etLat = findViewById<EditText>(R.id.et_latitude)
        val etLon = findViewById<EditText>(R.id.et_longitude)
        val etTimezone = findViewById<EditText>(R.id.et_timezone)
        val tvTimezoneHint = findViewById<TextView>(R.id.tv_timezone_hint)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)
        val tvWeatherHeader = findViewById<TextView>(R.id.tv_weather_header)
        val tvWeatherData = findViewById<TextView>(R.id.tv_weather_data)

        etLocation.setText(prefs.locationName)
        etLat.setText(prefs.latitude)
        etLon.setText(prefs.longitude)
        etTimezone.setText(prefs.timezone)

        val availableZones = TimeZone.getAvailableIDs().sorted().joinToString(", ")
        tvTimezoneHint.text = "Available: $availableZones"

        displayWeather(tvWeatherHeader, tvWeatherData)

        btnSave.setOnClickListener {
            val lat = etLat.text.toString().trim()
            val lon = etLon.text.toString().trim()
            val tz = etTimezone.text.toString().trim()
            val loc = etLocation.text.toString().trim()

            if (lat.toDoubleOrNull() == null) {
                Toast.makeText(this, "Invalid latitude", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lon.toDoubleOrNull() == null) {
                Toast.makeText(this, "Invalid longitude", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TimeZone.getAvailableIDs().none { it == tz }) {
                Toast.makeText(this, "Unknown timezone: $tz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (loc.isBlank()) {
                Toast.makeText(this, "Location name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.latitude = lat
            prefs.longitude = lon
            prefs.timezone = tz
            prefs.locationName = loc

            hideKeyboard(it)
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        btnRefresh.setOnClickListener {
            val lat = etLat.text.toString().trim()
            val lon = etLon.text.toString().trim()
            val tz = etTimezone.text.toString().trim()
            val loc = etLocation.text.toString().trim()

            if (lat.toDoubleOrNull() == null || lon.toDoubleOrNull() == null) {
                Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TimeZone.getAvailableIDs().none { it == tz }) {
                Toast.makeText(this, "Unknown timezone: $tz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (loc.isBlank()) {
                Toast.makeText(this, "Location name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRefresh.isEnabled = false
            tvWeatherHeader.text = "FETCHING..."
            tvWeatherData.text = ""
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val repo = WeatherRepository()
                    val result = repo.fetch(lat, lon, tz, loc)
                    prefs.cachedJson = Gson().toJson(result)
                    withContext(Dispatchers.Main) {
                        displayWeather(tvWeatherHeader, tvWeatherData)
                        btnRefresh.isEnabled = true
                        Toast.makeText(this@SettingsActivity, "Updated", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvWeatherHeader.text = "FETCH FAILED"
                        btnRefresh.isEnabled = true
                        Toast.makeText(this@SettingsActivity, "Fetch failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun displayWeather(header: TextView, data: TextView) {
        val json = prefs.cachedJson
        if (json.isBlank()) {
            header.text = "-- NO DATA --"
            data.text = ""
            return
        }
        val result = try {
            Gson().fromJson(json, WeatherResult::class.java)
        } catch (e: Exception) {
            Log.w("SettingsActivity", "Failed to parse cached weather JSON", e)
            null
        }
        if (result == null) {
            header.text = "-- DATA ERROR --"
            data.text = ""
            return
        }
        header.text = "${result.locationName}  UPDATED: ${result.updatedAt}"
        data.text = buildWeatherText(result)
    }

    private fun buildWeatherText(result: WeatherResult): String {
        val sb = StringBuilder()
        val colHeader = "TIME   TEMP     HUM   P%   PREC     WIND       POLLEN  COSMIC"
        if (result.today.isNotEmpty()) {
            sb.appendLine("-- TODAY --")
            sb.appendLine(colHeader)
            result.today.forEach { sb.appendLine(formatEntry(it)) }
        }
        if (result.tomorrow.isNotEmpty()) {
            sb.appendLine("-- TOMORROW --")
            sb.appendLine(colHeader)
            result.tomorrow.forEach { sb.appendLine(formatEntry(it)) }
        }
        return sb.toString().trimEnd()
    }

    private fun formatEntry(e: HourlyEntry): String {
        val temp = if (e.temperature.isNaN()) "   ---" else "%+6.1fC".format(e.temperature)
        val hum = if (e.humidity < 0) " ---" else "%3d%%".format(e.humidity)
        val pp = if (e.precipProb < 0) " ---" else "%3d%%".format(e.precipProb)
        val prec = if (e.precip.isNaN()) "    ---" else "%6.1fmm".format(e.precip)
        val wind = if (e.windSpeed.isNaN()) "      ---" else "%8.1fkm/h".format(e.windSpeed)
        val pol = if (e.pollen.isNaN()) "   ---" else "%6.0f".format(e.pollen)
        val cos = if (e.cosmicRay.isNaN()) "   ---" else "%6.0f".format(e.cosmicRay)
        return "${e.time}  $temp  $hum  $pp  $prec  $wind  $pol  $cos"
    }

    private fun hideKeyboard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }
}
