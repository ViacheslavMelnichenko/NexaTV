/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * Current conditions plus the day's range for the configured location, in whole degrees Celsius.
 * [weatherCode] is the WMO code Open-Meteo returns (drives the condition glyph); [windKmh] is the
 * current wind speed rounded to whole km/h. Both default to 0 so older/sparse payloads still map.
 * Android-free so the Open-Meteo mapping is unit-testable without a device or network (ADR-0010).
 */
data class Weather(
    val currentC: Int,
    val minC: Int,
    val maxC: Int,
    val weatherCode: Int = 0,
    val windKmh: Int = 0,
    val sunriseEpochSeconds: Long? = null,
    val sunsetEpochSeconds: Long? = null,
    val observedEpochSeconds: Long? = null,
)

/**
 * The Home weather widget's data source (ADR-0010). One keyless HTTPS GET to Open-Meteo, parsed with
 * [org.json]; no API key (no secret in Git), no new HTTP dependency. The location is a fixed city
 * constant (no GPS / location permission). [fetch] runs off the main thread and returns `null` on any
 * failure, so the widget degrades to "unavailable" rather than ever breaking Home.
 */
class WeatherRepository(
    private val latitude: Double = WROCLAW_LAT,
    private val longitude: Double = WROCLAW_LON,
) {

    /** One request on [Dispatchers.IO]; `null` on offline/timeout/HTTP-error/bad-payload (fail-soft). */
    suspend fun fetch(): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(endpoint(latitude, longitude)).openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                parseWeather(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    companion object {
        /** Fixed location for v1 (ADR-0010): Wrocław, Poland. A user-chosen city is a future setting. */
        const val WROCLAW_LAT = 51.1079
        const val WROCLAW_LON = 17.0385
        private const val TIMEOUT_MS = 8_000
    }
}

/**
 * The Open-Meteo request URL: current temperature, condition code and wind, plus today's min/max,
 * in the device's timezone. Wind is returned in km/h (Open-Meteo's default for `wind_speed_10m`).
 */
internal fun endpoint(latitude: Double, longitude: Double): String =
    "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
        "&current=temperature_2m,weather_code,wind_speed_10m" +
        "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset" +
        "&timezone=auto&timeformat=unixtime&forecast_days=1"

/**
 * Pure Open-Meteo JSON → [Weather]; `null` when temperature/min/max are missing or malformed. The
 * condition code and wind are optional (default 0) so a sparse payload still yields a usable reading.
 * Extracted and Android-free so it is unit-tested against sample payloads with no network (ADR-0010).
 */
internal fun parseWeather(json: String): Weather? = runCatching {
    val root = JSONObject(json)
    val current = root.getJSONObject("current")
    val temperature = current.getDouble("temperature_2m")
    val daily = root.getJSONObject("daily")
    val max = daily.getJSONArray("temperature_2m_max").getDouble(0)
    val min = daily.getJSONArray("temperature_2m_min").getDouble(0)
    Weather(
        currentC = temperature.roundToInt(),
        minC = min.roundToInt(),
        maxC = max.roundToInt(),
        weatherCode = current.optInt("weather_code", 0),
        windKmh = current.optDouble("wind_speed_10m", 0.0).roundToInt(),
        sunriseEpochSeconds = daily.optJSONArray("sunrise")?.optLong(0)?.takeIf { it > 0 },
        sunsetEpochSeconds = daily.optJSONArray("sunset")?.optLong(0)?.takeIf { it > 0 },
        observedEpochSeconds = current.optLong("time").takeIf { it > 0 },
    )
}.getOrNull()
