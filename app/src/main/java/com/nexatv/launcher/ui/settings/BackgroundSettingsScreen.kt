// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexatv.launcher.R
import com.nexatv.launcher.data.BackgroundBlur
import com.nexatv.launcher.data.BackgroundMode
import com.nexatv.launcher.data.BackgroundSettings
import com.nexatv.launcher.data.DayPeriod
import com.nexatv.launcher.data.SceneWeather
import com.nexatv.launcher.ui.theme.NexaTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun BackgroundSettingsScreen(
    settings: BackgroundSettings,
    onChange: (BackgroundSettings) -> Unit,
    onPreview: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Column(
        modifier = Modifier.fillMaxSize()
            .background(NexaTheme.colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(NexaTheme.spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.background_title), style = MaterialTheme.typography.headlineLarge,
                color = NexaTheme.colors.onSurface)
            SettingOptionChip(stringResource(R.string.background_preview), false, onPreview,
                Modifier.focusRequester(first))
        }
        Text(stringResource(R.string.background_description), style = MaterialTheme.typography.bodyMedium,
            color = NexaTheme.colors.onSurfaceMuted)
        ChoiceRow(R.string.background_mode, BackgroundMode.entries, settings.mode, ::modeLabel) {
            onChange(settings.copy(mode = it))
        }
        if (settings.mode == BackgroundMode.MANUAL) {
            ChoiceRow(R.string.background_period, DayPeriod.entries, settings.period, ::periodLabel) {
                onChange(settings.copy(period = it))
            }
            ChoiceRow(R.string.background_weather, SceneWeather.entries, settings.weather, ::weatherLabel) {
                onChange(settings.copy(weather = it))
            }
        }
        if (settings.mode != BackgroundMode.THEME) {
            ChoiceRow(R.string.background_blur, BackgroundBlur.entries, settings.blur, ::blurLabel) {
                onChange(settings.copy(blur = it))
            }
            SettingRow(stringResource(R.string.background_dim)) {
                listOf(0, 15, 25, 40, 60).forEach { dim ->
                    SettingOptionChip(stringResource(R.string.background_dim_percent, dim), settings.dimPercent == dim,
                        { onChange(settings.copy(dimPercent = dim)) })
                }
            }
            SettingRow(stringResource(R.string.background_motion)) {
                SettingOptionChip(stringResource(R.string.background_off), !settings.motion,
                    { onChange(settings.copy(motion = false)) })
                SettingOptionChip(stringResource(R.string.background_on), settings.motion,
                    { onChange(settings.copy(motion = true)) })
            }
            Text(stringResource(R.string.background_motion_hint), style = MaterialTheme.typography.bodyMedium,
                color = NexaTheme.colors.onSurfaceMuted)
        }
    }
    LaunchedEffect(Unit) { first.requestFocus() }
}

@Composable
private fun <T> ChoiceRow(label: Int, values: List<T>, selected: T, labelFor: (T) -> Int, onSelect: (T) -> Unit) {
    SettingRow(stringResource(label)) {
        values.forEach { value ->
            SettingOptionChip(stringResource(labelFor(value)), value == selected, { onSelect(value) })
        }
    }
}

private fun modeLabel(mode: BackgroundMode): Int = when (mode) {
    BackgroundMode.THEME -> R.string.background_theme
    BackgroundMode.ADAPTIVE -> R.string.background_adaptive
    BackgroundMode.MANUAL -> R.string.background_manual
}

private fun periodLabel(period: DayPeriod): Int = when (period) {
    DayPeriod.DAWN -> R.string.background_dawn
    DayPeriod.DAY -> R.string.background_day
    DayPeriod.SUNSET -> R.string.background_sunset
    DayPeriod.NIGHT -> R.string.background_night
}

private fun weatherLabel(weather: SceneWeather): Int = when (weather) {
    SceneWeather.CLEAR -> R.string.background_clear
    SceneWeather.CLOUDY -> R.string.background_cloudy
    SceneWeather.RAIN -> R.string.background_rain
    SceneWeather.SNOW -> R.string.background_snow
}

private fun blurLabel(blur: BackgroundBlur): Int = when (blur) {
    BackgroundBlur.NONE -> R.string.background_off
    BackgroundBlur.SOFT -> R.string.background_blur_soft
    BackgroundBlur.STRONG -> R.string.background_blur_strong
}
