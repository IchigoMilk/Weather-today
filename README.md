# Weather Today

An Android home screen widget that displays hourly weather and space data for today and tomorrow.

## Features

- Hourly forecast (temperature, humidity, precipitation probability, precipitation amount, wind speed)
- Pollen density (birch, grass, and alder pollen combined) via Open-Meteo Air Quality API
- Cosmic ray intensity via NMDB (Neutron Monitor Database, Oulu station)
- Hacker-style black terminal display with monospace font
- User-configurable location (latitude/longitude), location name, and timezone
- Auto-refresh every 30 minutes; manual refresh from widget or settings screen
- Refreshes on device boot

## Data Sources

- Weather and air quality: [Open-Meteo](https://open-meteo.com/) (free, no API key required)
- Cosmic rays: [NMDB Oulu station](https://www.nmdb.eu/) (free, no API key required)

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection

## Build

1. Install Android Studio or the Android command-line tools.
2. The Gradle wrapper JAR is required. If missing, run:

   ```
   gradle wrapper --gradle-version 8.7
   ```

3. Build the APK:

   ```
   ./gradlew assembleDebug
   ```

   The APK is output to `app/build/outputs/apk/debug/`.

## Settings

Open the app (launcher icon) to configure:

- Location Name: display label shown on the widget header
- Latitude: decimal degrees (e.g., 35.6895 for Tokyo)
- Longitude: decimal degrees (e.g., 139.6917 for Tokyo)
- Timezone: IANA timezone string (e.g., Asia/Tokyo, America/New_York, Europe/Berlin)

Tap "SAVE" to apply, then "REFRESH WIDGET" to force an immediate data fetch.
You can also tap "[REFRESH]" on the widget itself.

## Widget Columns

```
TIME  TEMP   HUM  P%   PREC   WIND   POLLEN  COSMIC
```

- TIME: hour (HH:mm)
- TEMP: temperature in Celsius
- HUM: relative humidity (%)
- P%: precipitation probability (%)
- PREC: precipitation amount (mm)
- WIND: wind speed (km/h)
- POLLEN: total pollen count (grains/m3, sum of birch+grass+alder)
- COSMIC: neutron count rate from Oulu station (counts per minute); shown as "---" if data is unavailable

## Notes

- Cosmic ray data from NMDB may be delayed or unavailable; "---" is shown in that case.
- Pollen data availability depends on region and season.
- The widget uses the default location (Tokyo) until you change it in settings.

## License

MIT
