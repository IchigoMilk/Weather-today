package io.github.ichigomilk.weathertoday

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WeatherRepository {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    fun fetch(lat: String, lon: String, timezone: String, locationName: String): WeatherResult {
        val weatherJson = fetchWeather(lat, lon, timezone)
        val aqJson = fetchAirQuality(lat, lon, timezone)
        val cosmicRays = fetchCosmicRays()
        return merge(weatherJson, aqJson, cosmicRays, locationName, timezone)
    }

    private fun JsonElement?.safeDouble(): Double =
        if (this == null || isJsonNull) Double.NaN else try { asDouble } catch (_: Exception) { Double.NaN }

    private fun JsonElement?.safeInt(): Int =
        if (this == null || isJsonNull) -1 else try { asInt } catch (_: Exception) { -1 }

    private fun JsonElement?.safeString(): String? =
        if (this == null || isJsonNull) null else try { asString } catch (_: Exception) { null }

    private fun get(url: String): String {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return "{}"
            return resp.body?.string() ?: "{}"
        }
    }

    private fun fetchWeather(lat: String, lon: String, timezone: String): JsonObject {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&hourly=temperature_2m,relativehumidity_2m,precipitation_probability," +
            "precipitation,windspeed_10m" +
            "&timezone=${timezone.replace("/", "%2F")}" +
            "&forecast_days=2"
        return try {
            gson.fromJson(get(url), JsonObject::class.java) ?: JsonObject()
        } catch (e: Exception) {
            JsonObject()
        }
    }

    private fun fetchAirQuality(lat: String, lon: String, timezone: String): JsonObject {
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=$lat&longitude=$lon" +
            "&hourly=birch_pollen,grass_pollen,alder_pollen" +
            "&timezone=${timezone.replace("/", "%2F")}" +
            "&forecast_days=2"
        return try {
            gson.fromJson(get(url), JsonObject::class.java) ?: JsonObject()
        } catch (e: Exception) {
            JsonObject()
        }
    }

    private fun fetchCosmicRays(): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        try {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val startDate = "${fmt.format(today)}%2000%3A00%3A00"
            val endDate = "${fmt.format(tomorrow)}%2023%3A00%3A00"
            val url = "https://www.nmdb.eu/nest/api.php?output=ascii" +
                "&stations%5B%5D=OULU" +
                "&startdate=$startDate" +
                "&enddate=$endDate" +
                "&resolution=60"
            val body = get(url)
            for (line in body.lines()) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("start_date_time")) continue
                val parts = line.split(";")
                if (parts.size >= 2) {
                    val timeStr = parts[0].trim().replace(" ", "T").take(16)
                    val count = parts[parts.size - 1].trim().toDoubleOrNull() ?: continue
                    result[timeStr] = count
                }
            }
        } catch (e: Exception) {
            Log.w("WeatherRepository", "Cosmic ray fetch failed; will show '---'", e)
        }
        return result
    }

    private fun merge(
        weather: JsonObject,
        aq: JsonObject,
        cosmicRays: Map<String, Double>,
        locationName: String,
        timezone: String
    ): WeatherResult {
        val zoneId = try { ZoneId.of(timezone) } catch (_: Exception) { ZoneId.systemDefault() }
        val now = LocalDateTime.now(zoneId)
        val todayDate = now.toLocalDate()
        val tomorrowDate = todayDate.plusDays(1)
        val updatedAt = now.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))

        val times = weather.getAsJsonObject("hourly")
            ?.getAsJsonArray("time")?.mapNotNull { it.safeString() } ?: emptyList()
        val temps = weather.getAsJsonObject("hourly")
            ?.getAsJsonArray("temperature_2m")?.map { it.safeDouble() } ?: emptyList()
        val humids = weather.getAsJsonObject("hourly")
            ?.getAsJsonArray("relativehumidity_2m")?.map { it.safeInt() } ?: emptyList()
        val precipProbs = weather.getAsJsonObject("hourly")
            ?.getAsJsonArray("precipitation_probability")?.map { it.safeInt() } ?: emptyList()
        val precips = weather.getAsJsonObject("hourly")
            ?.getAsJsonArray("precipitation")?.map { it.safeDouble() } ?: emptyList()
        val winds = weather.getAsJsonObject("hourly")
            ?.getAsJsonArray("windspeed_10m")?.map { it.safeDouble() } ?: emptyList()

        val aqTimes = aq.getAsJsonObject("hourly")
            ?.getAsJsonArray("time")?.mapNotNull { it.safeString() } ?: emptyList()
        val birch = aq.getAsJsonObject("hourly")
            ?.getAsJsonArray("birch_pollen")?.map { it.safeDouble() } ?: emptyList()
        val grass = aq.getAsJsonObject("hourly")
            ?.getAsJsonArray("grass_pollen")?.map { it.safeDouble() } ?: emptyList()
        val alder = aq.getAsJsonObject("hourly")
            ?.getAsJsonArray("alder_pollen")?.map { it.safeDouble() } ?: emptyList()
        val pollenMap = aqTimes.mapIndexed { i, t ->
            val b = birch.getOrElse(i) { Double.NaN }.let { if (it.isNaN()) 0.0 else it }
            val g = grass.getOrElse(i) { Double.NaN }.let { if (it.isNaN()) 0.0 else it }
            val al = alder.getOrElse(i) { Double.NaN }.let { if (it.isNaN()) 0.0 else it }
            t to (b + g + al)
        }.toMap()

        val today = mutableListOf<HourlyEntry>()
        val tomorrow = mutableListOf<HourlyEntry>()

        for (i in times.indices) {
            val t = times[i]
            val dt = try { LocalDateTime.parse(t, iso) } catch (_: Exception) { continue }
            val date = dt.toLocalDate()
            val timeLabel = t.takeLast(5)
            val entry = HourlyEntry(
                time = timeLabel,
                temperature = temps.getOrElse(i) { Double.NaN },
                humidity = humids.getOrElse(i) { -1 },
                precipProb = precipProbs.getOrElse(i) { -1 },
                precip = precips.getOrElse(i) { Double.NaN },
                windSpeed = winds.getOrElse(i) { Double.NaN },
                pollen = pollenMap[t] ?: Double.NaN,
                cosmicRay = cosmicRays[t.take(16)] ?: Double.NaN
            )
            when (date) {
                todayDate -> today.add(entry)
                tomorrowDate -> tomorrow.add(entry)
            }
        }

        return WeatherResult(
            locationName = locationName,
            timezone = timezone,
            today = today,
            tomorrow = tomorrow,
            updatedAt = updatedAt
        )
    }
}
