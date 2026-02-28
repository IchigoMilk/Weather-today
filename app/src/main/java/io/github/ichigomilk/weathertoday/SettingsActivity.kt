package io.github.ichigomilk.weathertoday

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        etLocation.setText(prefs.locationName)
        etLat.setText(prefs.latitude)
        etLon.setText(prefs.longitude)
        etTimezone.setText(prefs.timezone)

        val availableZones = TimeZone.getAvailableIDs().sorted().joinToString(", ")
        tvTimezoneHint.text = "Available: $availableZones"

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
            val manager = AppWidgetManager.getInstance(this)
            val ids = manager.getAppWidgetIds(ComponentName(this, WeatherWidget::class.java))
            if (ids.isEmpty()) {
                Toast.makeText(this, "No widget added yet", Toast.LENGTH_SHORT).show()
            } else {
                val intent = android.content.Intent(this, WeatherWidget::class.java).apply {
                    action = ACTION_REFRESH
                }
                sendBroadcast(intent)
                Toast.makeText(this, "Refresh triggered", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideKeyboard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }
}
