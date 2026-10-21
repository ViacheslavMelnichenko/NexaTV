// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.ui.home

import com.nexatv.launcher.data.BackgroundMode
import com.nexatv.launcher.data.BackgroundSettings
import com.nexatv.launcher.data.DayPeriod
import com.nexatv.launcher.data.SceneWeather
import com.nexatv.launcher.data.Weather
import java.io.File
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneResolverTest {
    private val day = LocalDate.of(2026, 10, 21)
    private fun at(hour: Int, minute: Int = 0): Instant = day.atTime(hour, minute).atZone(WEATHER_ZONE).toInstant()
    private val weather = Weather(12, 9, 15, 61, sunriseEpochSeconds = at(7, 30).epochSecond,
        sunsetEpochSeconds = at(17, 45).epochSecond)

    @Test
    fun `period boundaries use the city sunrise and sunset`() {
        assertEquals(DayPeriod.NIGHT, resolvePeriod(at(6, 59), weather))
        assertEquals(DayPeriod.DAWN, resolvePeriod(at(7), weather))
        assertEquals(DayPeriod.DAY, resolvePeriod(at(8, 15), weather))
        assertEquals(DayPeriod.SUNSET, resolvePeriod(at(16, 45), weather))
        assertEquals(DayPeriod.NIGHT, resolvePeriod(at(18, 30), weather))
    }

    @Test
    fun `yesterdays solar times cannot make tomorrow permanently night`() {
        val tomorrow = at(12).plusSeconds(24 * 60 * 60)
        assertEquals(DayPeriod.DAY, resolvePeriod(tomorrow, weather))
        assertEquals(DayPeriod.NIGHT, resolvePeriod(at(23), null))
    }

    @Test
    fun `manual scene ignores real weather and time`() {
        val settings = BackgroundSettings(mode = BackgroundMode.MANUAL, period = DayPeriod.DAY,
            weather = SceneWeather.SNOW)
        assertEquals(BackgroundScene(DayPeriod.DAY, SceneWeather.SNOW), resolveScene(settings, at(23), weather))
    }

    @Test
    fun `weather codes group rain snow fog thunderstorms and missing data`() {
        listOf(51, 55, 57, 61, 65, 67, 80, 82, 95, 96, 99).forEach {
            assertEquals(SceneWeather.RAIN, sceneWeather(it))
        }
        listOf(71, 73, 75, 77, 85, 86).forEach { assertEquals(SceneWeather.SNOW, sceneWeather(it)) }
        listOf(2, 3, 45, 48, -1, null).forEach { assertEquals(SceneWeather.CLOUDY, sceneWeather(it)) }
        assertEquals(SceneWeather.CLEAR, sceneWeather(0))
    }

    @Test
    fun `stale observations and clock corrections fall back instead of showing old weather`() {
        assertEquals(weather, freshWeather(weather, at(10), at(12)))
        assertNull(freshWeather(weather, at(8), at(12)))
        assertNull(freshWeather(weather, at(13), at(12)))
        assertNull(freshWeather(weather.copy(observedEpochSeconds = at(6).epochSecond), at(12), at(12)))
    }

    @Test
    fun `every selectable scene has a bundled asset within the artwork budget`() {
        val files = DayPeriod.entries.flatMap { period ->
            SceneWeather.entries.map { condition -> File("src/main/assets/" + BackgroundScene(period, condition).asset) }
        }
        files.forEach { assertTrue("Missing scene: $it", it.isFile && it.length() > 0) }
        assertTrue("Artwork exceeds 5 MiB budget", files.sumOf { it.length() } <= 5 * 1024 * 1024)
    }
}
