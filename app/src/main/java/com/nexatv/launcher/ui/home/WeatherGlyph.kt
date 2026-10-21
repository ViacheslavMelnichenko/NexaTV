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

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The weather conditions we draw a glyph for, collapsed from Open-Meteo's WMO codes (ADR-0010). */
internal enum class WeatherCondition { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

/** Collapse a WMO weather code (Open-Meteo `weather_code`) to the condition glyph we draw for it. */
internal fun wmoToCondition(code: Int): WeatherCondition = when (code) {
    0 -> WeatherCondition.CLEAR
    1, 2 -> WeatherCondition.PARTLY
    3 -> WeatherCondition.CLOUDY
    45, 48 -> WeatherCondition.FOG
    in 71..77, 85, 86 -> WeatherCondition.SNOW
    in 95..99 -> WeatherCondition.STORM
    // 51..67 drizzle/rain, 80..82 showers, and any unknown code fall through to rain.
    else -> WeatherCondition.RAIN
}

/**
 * A monochrome, resolution-independent weather glyph drawn with [Canvas] primitives — no bitmap assets
 * and no icon dependency, so it stays crisp at any tile size and restyles with the theme [tint].
 */
@Composable
internal fun WeatherGlyph(condition: WeatherCondition, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (condition) {
            WeatherCondition.CLEAR ->
                sun(center = Offset(w / 2f, h / 2f), radius = size.minDimension * 0.22f, color = tint)
            WeatherCondition.PARTLY -> {
                sun(center = Offset(w * 0.40f, h * 0.38f), radius = size.minDimension * 0.16f, color = tint)
                cloud(center = Offset(w * 0.56f, h * 0.60f), width = w * 0.60f, color = tint)
            }
            WeatherCondition.CLOUDY ->
                cloud(center = Offset(w / 2f, h * 0.52f), width = w * 0.72f, color = tint)
            WeatherCondition.FOG -> {
                cloud(center = Offset(w / 2f, h * 0.44f), width = w * 0.72f, color = tint)
                fogLines(color = tint)
            }
            WeatherCondition.RAIN -> {
                cloud(center = Offset(w / 2f, h * 0.44f), width = w * 0.72f, color = tint)
                rain(color = tint)
            }
            WeatherCondition.SNOW -> {
                cloud(center = Offset(w / 2f, h * 0.44f), width = w * 0.72f, color = tint)
                snow(color = tint)
            }
            WeatherCondition.STORM -> {
                cloud(center = Offset(w / 2f, h * 0.44f), width = w * 0.72f, color = tint)
                bolt(color = tint)
            }
        }
    }
}

/** A small wind glyph: two stylised gusts, used beside the wind-speed readout. */
@Composable
internal fun WindGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.10f
        gust(y = size.height * 0.38f, length = 0.64f, color = tint, stroke = stroke)
        gust(y = size.height * 0.62f, length = 0.78f, color = tint, stroke = stroke)
    }
}

private fun DrawScope.sun(center: Offset, radius: Float, color: Color) {
    drawCircle(color = color, radius = radius, center = center)
    val inner = radius * 1.35f
    val outer = inner + radius * 0.5f
    repeat(RAY_COUNT) { i ->
        val angle = (PI / (RAY_COUNT / 2) * i).toFloat()
        val dx = cos(angle)
        val dy = sin(angle)
        drawLine(
            color = color,
            start = Offset(center.x + dx * inner, center.y + dy * inner),
            end = Offset(center.x + dx * outer, center.y + dy * outer),
            strokeWidth = radius * 0.22f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.cloud(center: Offset, width: Float, color: Color) {
    val h = width * 0.62f
    val left = center.x - width / 2f
    val bottom = center.y + h / 2f
    val top = center.y - h / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(left, bottom - h * 0.5f),
        size = Size(width, h * 0.5f),
        cornerRadius = CornerRadius(h * 0.22f, h * 0.22f),
    )
    drawCircle(color = color, radius = h * 0.32f, center = Offset(left + width * 0.26f, bottom - h * 0.42f))
    drawCircle(color = color, radius = h * 0.46f, center = Offset(center.x, top + h * 0.44f))
    drawCircle(color = color, radius = h * 0.30f, center = Offset(left + width * 0.78f, bottom - h * 0.44f))
}

private fun DrawScope.rain(color: Color) {
    val top = size.height * 0.70f
    val bottom = size.height * 0.90f
    listOf(0.38f, 0.54f, 0.70f).forEach { fx ->
        val x = size.width * fx
        drawLine(
            color = color,
            start = Offset(x, top),
            end = Offset(x - size.width * 0.05f, bottom),
            strokeWidth = size.minDimension * 0.06f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.snow(color: Color) {
    val y = size.height * 0.82f
    listOf(0.40f, 0.54f, 0.68f).forEach { fx ->
        drawCircle(color = color, radius = size.minDimension * 0.045f, center = Offset(size.width * fx, y))
    }
}

private fun DrawScope.fogLines(color: Color) {
    val startX = size.width * 0.28f
    val endX = size.width * 0.72f
    listOf(0.74f, 0.86f).forEach { fy ->
        val y = size.height * fy
        drawLine(
            color = color,
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = size.minDimension * 0.06f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.bolt(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.52f, h * 0.68f)
        lineTo(w * 0.44f, h * 0.84f)
        lineTo(w * 0.52f, h * 0.84f)
        lineTo(w * 0.46f, h * 0.98f)
        lineTo(w * 0.62f, h * 0.80f)
        lineTo(w * 0.53f, h * 0.80f)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.gust(y: Float, length: Float, color: Color, stroke: Float) {
    val w = size.width
    val path = Path().apply {
        moveTo(w * 0.12f, y)
        lineTo(w * length, y)
        lineTo(w * (length + 0.07f), y - size.height * 0.13f)
    }
    drawPath(path = path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

/** Rays around the clear-sky sun; kept even so opposite rays line up through the centre. */
private const val RAY_COUNT = 8
