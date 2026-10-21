// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.ui.home

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.nexatv.launcher.data.SceneWeather
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

/** Optional 20 Hz decorative layer. Animated state is read only in draw, never by the widget tree. */
@Composable
internal fun WeatherParticles(weather: SceneWeather) {
    if (weather != SceneWeather.RAIN && weather != SceneWeather.SNOW) return
    val time = remember { mutableFloatStateOf(0f) }
    val seeds = remember {
        val random = Random(37)
        List(36) { Offset(random.nextFloat(), random.nextFloat()) }
    }
    LaunchedEffect(weather) {
        val start = SystemClock.uptimeMillis()
        while (isActive) {
            time.floatValue = (SystemClock.uptimeMillis() - start) / 1000f
            delay(50L)
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val snow = weather == SceneWeather.SNOW
        val seconds = time.floatValue
        seeds.forEachIndexed { index, seed ->
            val speed = if (snow) 0.025f else 0.24f
            val y = (seed.y + seconds * speed * (0.7f + seed.x)) % 1f
            val drift = if (snow) sin(seconds * 0.3f + index) * 0.015f else -y * 0.025f
            val x = (seed.x + drift + 1f) % 1f
            // Keep the central cards quiet; motion is most visible at the margins.
            val alpha = if (x in 0.18f..0.82f && y in 0.18f..0.8f) 0.07f else 0.22f
            val position = Offset(x * size.width, y * size.height)
            if (snow) {
                drawCircle(Color.White.copy(alpha = alpha), size.width * 0.0012f, position)
            } else {
                drawLine(Color.White.copy(alpha = alpha), position,
                    position + Offset(-size.width * 0.0015f, size.height * 0.014f), size.width * 0.0006f)
            }
        }
    }
}
