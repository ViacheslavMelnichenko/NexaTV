/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nexatv.launcher.R
import com.nexatv.launcher.data.Weather
import com.nexatv.launcher.ui.theme.NexaTheme
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Fixed height so the two centre cards read as one balanced, equal-sized pair (ADR-0010). */
private val WIDGET_HEIGHT = 220.dp

/** Fixed width so the pair stays a compact, centred cluster rather than stretching across the row. */
private val WIDGET_WIDTH = 280.dp

/**
 * The centre of Home (ADR-0010): two equal-width cards — an offline month calendar with today
 * highlighted, and a local weather card for the configured city. Purely informational, so neither is
 * focusable; a D-pad ▲ from the favorites row passes through them to the top bar.
 */
@Composable
internal fun HomeWidgets(environment: HomeEnvironment, landscape: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.lg, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarWidget(landscape = landscape, modifier = Modifier.width(WIDGET_WIDTH))
        WeatherWidget(environment, landscape, modifier = Modifier.width(WIDGET_WIDTH))
    }
}

/** The shared card chrome: a fixed-height, rounded surface both widgets fill. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WidgetCard(
    landscape: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.height(WIDGET_HEIGHT),
        shape = RoundedCornerShape(NexaTheme.radius.md),
        colors = SurfaceDefaults.colors(
            containerColor = NexaTheme.colors.surfaceVariant.copy(alpha = if (landscape) 0.88f else 1f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(NexaTheme.spacing.md),
            content = content,
        )
    }
}

/** A month calendar for the current month with today highlighted; fully offline. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CalendarWidget(landscape: Boolean, modifier: Modifier = Modifier) {
    val calendar = remember { Calendar.getInstance() }
    val today = remember { calendar.get(Calendar.DAY_OF_MONTH) }
    val monthLabel = remember {
        SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(calendar.time)
            .replaceFirstChar { it.uppercase() }
    }
    val headers = remember { weekdayHeaders(calendar) }
    val weeks = remember { monthMatrix(calendar) }

    WidgetCard(landscape = landscape, modifier = modifier) {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            color = NexaTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.height(NexaTheme.spacing.sm))
        Row(modifier = Modifier.fillMaxWidth()) {
            headers.forEach { label -> CalendarCell(text = label, muted = true) }
        }
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    week.forEach { day ->
                        CalendarDay(day = day, isToday = day == today)
                    }
                }
            }
        }
    }
}

/** One weekday-header cell (equal width across the row). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RowScope.CalendarCell(text: String, muted: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (muted) NexaTheme.colors.onSurfaceMuted else NexaTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f),
    )
}

/** One day cell; [day] null = padding before the 1st / after the last. Today gets an accent disc. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RowScope.CalendarDay(day: Int?, isToday: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        if (day != null) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isToday) NexaTheme.colors.accent else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) NexaTheme.colors.onAccent else NexaTheme.colors.onSurface,
                )
            }
        }
    }
}

/** Local weather (ADR-0010): fetched lazily, fail-soft — "unavailable" never blocks or crashes Home. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WeatherWidget(environment: HomeEnvironment, landscape: Boolean, modifier: Modifier = Modifier) {
    WidgetCard(landscape = landscape, modifier = modifier) {
        Text(
            text = stringResource(R.string.home_weather_location),
            style = MaterialTheme.typography.titleMedium,
            color = NexaTheme.colors.onSurface,
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val current = environment.weather
            when {
                current != null -> WeatherReadout(current)
                environment.loaded -> Text(
                    text = stringResource(R.string.home_weather_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexaTheme.colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The condition glyph and current temperature, over the day's low/high and the wind speed. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WeatherReadout(weather: Weather) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.sm),
        ) {
            WeatherGlyph(
                condition = wmoToCondition(weather.weatherCode),
                tint = NexaTheme.colors.onSurface,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text = "${weather.currentC}°",
                style = MaterialTheme.typography.displayMedium,
                color = NexaTheme.colors.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(NexaTheme.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md)) {
            Text(
                text = "↓ ${weather.minC}°",
                style = MaterialTheme.typography.bodyLarge,
                color = NexaTheme.colors.onSurfaceMuted,
            )
            Text(
                text = "↑ ${weather.maxC}°",
                style = MaterialTheme.typography.bodyLarge,
                color = NexaTheme.colors.onSurfaceMuted,
            )
        }
        Spacer(modifier = Modifier.height(NexaTheme.spacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.xs),
        ) {
            WindGlyph(
                tint = NexaTheme.colors.onSurfaceMuted,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "${weather.windKmh} km/h",
                style = MaterialTheme.typography.bodyMedium,
                color = NexaTheme.colors.onSurfaceMuted,
            )
        }
    }
}

/** Weeks of the current month as day numbers; null cells pad before the 1st and after the last. */
private fun monthMatrix(calendar: Calendar): List<List<Int?>> {
    val first = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val lead = ((first.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek) + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val cells = buildList {
        repeat(lead) { add(null) }
        for (day in 1..daysInMonth) add(day)
        while (size % DAYS_IN_WEEK != 0) add(null)
    }
    return cells.chunked(DAYS_IN_WEEK)
}

/** Short weekday labels ordered by the locale's first day of week (e.g. Mon…Sun or Sun…Sat). */
private fun weekdayHeaders(calendar: Calendar): List<String> {
    val shorts = DateFormatSymbols.getInstance().shortWeekdays // 1-indexed: [_, Sun, Mon, … Sat]
    return (0 until DAYS_IN_WEEK).map { offset ->
        val dayOfWeek = ((calendar.firstDayOfWeek - 1 + offset) % DAYS_IN_WEEK) + 1
        shorts[dayOfWeek].take(WEEKDAY_LABEL_CHARS)
    }
}

private const val DAYS_IN_WEEK = 7
private const val WEEKDAY_LABEL_CHARS = 2
