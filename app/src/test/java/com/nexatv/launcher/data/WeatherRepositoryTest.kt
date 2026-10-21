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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure Open-Meteo mapping (ADR-0010), tested against sample payloads with no network. Uses the real
 * org.json on the JVM test classpath (android.jar ships only stubs) — see `testImplementation(libs.json)`.
 */
class WeatherRepositoryTest {

    @Test
    fun `parses current temperature and rounds the day's min and max`() {
        val json = """
            {
              "current": { "temperature_2m": 12.6 },
              "daily": {
                "temperature_2m_max": [13.4],
                "temperature_2m_min": [4.8]
              }
            }
        """.trimIndent()

        assertEquals(Weather(currentC = 13, minC = 5, maxC = 13), parseWeather(json))
    }

    @Test
    fun `parses the WMO weather code and rounds wind speed when present`() {
        val json = """
            {
              "current": { "temperature_2m": 9.2, "weather_code": 2, "wind_speed_10m": 14.6 },
              "daily": {
                "temperature_2m_max": [19.1],
                "temperature_2m_min": [8.7]
              }
            }
        """.trimIndent()

        assertEquals(
            Weather(currentC = 9, minC = 9, maxC = 19, weatherCode = 2, windKmh = 15),
            parseWeather(json),
        )
    }

    @Test
    fun `defaults code and wind to zero when the payload omits them`() {
        val json = """
            {
              "current": { "temperature_2m": 5.0 },
              "daily": { "temperature_2m_max": [6.0], "temperature_2m_min": [1.0] }
            }
        """.trimIndent()

        assertEquals(Weather(currentC = 5, minC = 1, maxC = 6), parseWeather(json))
    }

    @Test
    fun `returns null on non-json input`() {
        assertNull(parseWeather("not json at all"))
    }

    @Test
    fun `returns null when required fields are missing`() {
        assertNull(parseWeather("""{"current":{},"daily":{}}"""))
    }

    @Test
    fun `endpoint carries the coordinates and the required current and daily fields`() {
        val url = endpoint(WeatherRepository.WROCLAW_LAT, WeatherRepository.WROCLAW_LON)

        assertTrue(url.startsWith("https://"))
        assertTrue(url.contains("latitude=51.1079"))
        assertTrue(url.contains("longitude=17.0385"))
        assertTrue(url.contains("current=temperature_2m"))
        assertTrue(url.contains("weather_code"))
        assertTrue(url.contains("wind_speed_10m"))
        assertTrue(url.contains("temperature_2m_max"))
        assertTrue(url.contains("temperature_2m_min"))
    }
}
