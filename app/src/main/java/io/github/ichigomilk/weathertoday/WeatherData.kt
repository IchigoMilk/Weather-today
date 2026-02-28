package io.github.ichigomilk.weathertoday

data class HourlyEntry(
    val time: String,
    val temperature: Double,
    val humidity: Int,
    val precipProb: Int,
    val precip: Double,
    val windSpeed: Double,
    val pollen: Double,
    val cosmicRay: Double
)

data class WeatherResult(
    val locationName: String,
    val timezone: String,
    val today: List<HourlyEntry>,
    val tomorrow: List<HourlyEntry>,
    val updatedAt: String
)
