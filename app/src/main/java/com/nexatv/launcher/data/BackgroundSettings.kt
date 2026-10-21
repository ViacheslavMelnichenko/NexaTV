// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.data

import com.nexatv.launcher.data.proto.BackgroundSettingsProto

enum class BackgroundMode { THEME, ADAPTIVE, MANUAL }
enum class DayPeriod { DAWN, DAY, SUNSET, NIGHT }
enum class SceneWeather { CLEAR, CLOUDY, RAIN, SNOW }
enum class BackgroundBlur { NONE, SOFT, STRONG }

data class BackgroundSettings(
    val mode: BackgroundMode = BackgroundMode.THEME,
    val period: DayPeriod = DayPeriod.DAY,
    val weather: SceneWeather = SceneWeather.CLEAR,
    val blur: BackgroundBlur = BackgroundBlur.SOFT,
    val dimPercent: Int = 25,
    val motion: Boolean = false,
)

internal fun BackgroundSettingsProto.toBackgroundSettings() = BackgroundSettings(
    mode = enumValueOrDefault(mode, BackgroundMode.THEME),
    period = enumValueOrDefault(period, DayPeriod.DAY),
    weather = enumValueOrDefault(weather, SceneWeather.CLEAR),
    blur = enumValueOrDefault(blur, BackgroundBlur.SOFT),
    dimPercent = dimPercent.coerceIn(0, 70),
    motion = motion,
)

internal fun BackgroundSettings.toProto(): BackgroundSettingsProto = BackgroundSettingsProto.newBuilder()
    .setMode(mode.name)
    .setPeriod(period.name)
    .setWeather(weather.name)
    .setBlur(blur.name)
    .setDimPercent(dimPercent.coerceIn(0, 70))
    .setMotion(motion)
    .build()

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback
