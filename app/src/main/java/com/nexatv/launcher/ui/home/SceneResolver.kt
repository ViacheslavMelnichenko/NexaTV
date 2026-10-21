// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.ui.home

import com.nexatv.launcher.data.BackgroundMode
import com.nexatv.launcher.data.BackgroundSettings
import com.nexatv.launcher.data.DayPeriod
import com.nexatv.launcher.data.SceneWeather
import com.nexatv.launcher.data.Weather
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Matches the weather repository's fixed city; device timezone must not shift Wroclaw's sunset. */
internal val WEATHER_ZONE: ZoneId = ZoneId.of("Europe/Warsaw")
internal const val WEATHER_MAX_AGE_SECONDS = 3 * 60 * 60L

internal data class BackgroundScene(val period: DayPeriod, val weather: SceneWeather) {
    val asset: String get() = "backgrounds/${period.name.lowercase(Locale.ROOT)}_" +
        "${weather.name.lowercase(Locale.ROOT)}.webp"
}

internal fun resolveScene(settings: BackgroundSettings, now: Instant, weather: Weather?): BackgroundScene =
    if (settings.mode == BackgroundMode.MANUAL) {
        BackgroundScene(settings.period, settings.weather)
    } else {
        BackgroundScene(resolvePeriod(now, weather), sceneWeather(weather?.weatherCode))
    }

internal fun sceneWeather(code: Int?): SceneWeather = when (code) {
    0, 1 -> SceneWeather.CLEAR
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99 -> SceneWeather.RAIN
    71, 73, 75, 77, 85, 86 -> SceneWeather.SNOW
    else -> SceneWeather.CLOUDY
}

internal fun resolvePeriod(now: Instant, weather: Weather?): DayPeriod {
    val date = now.atZone(WEATHER_ZONE).toLocalDate()
    val sunrise = weather?.sunriseEpochSeconds?.let(Instant::ofEpochSecond)
        ?.takeIf { it.atZone(WEATHER_ZONE).toLocalDate() == date }
        ?: date.atTime(6, 30).atZone(WEATHER_ZONE).toInstant()
    val sunset = weather?.sunsetEpochSeconds?.let(Instant::ofEpochSecond)
        ?.takeIf { it.atZone(WEATHER_ZONE).toLocalDate() == date && it > sunrise }
        ?: date.atTime(18, 30).atZone(WEATHER_ZONE).toInstant()
    return when {
        now < sunrise.minusSeconds(30 * 60) -> DayPeriod.NIGHT
        now < sunrise.plusSeconds(45 * 60) -> DayPeriod.DAWN
        now < sunset.minusSeconds(60 * 60) -> DayPeriod.DAY
        now < sunset.plusSeconds(45 * 60) -> DayPeriod.SUNSET
        else -> DayPeriod.NIGHT
    }
}

/** Reject stale observations and future timestamps after a clock correction. */
internal fun freshWeather(weather: Weather?, fetchedAt: Instant?, now: Instant): Weather? {
    if (weather == null || fetchedAt == null) return null
    val fetchedAge = now.epochSecond - fetchedAt.epochSecond
    val observedAge = weather.observedEpochSeconds?.let { now.epochSecond - it }
    return weather.takeIf {
        fetchedAge in 0..WEATHER_MAX_AGE_SECONDS &&
            (observedAge == null || observedAge in -300..WEATHER_MAX_AGE_SECONDS)
    }
}
