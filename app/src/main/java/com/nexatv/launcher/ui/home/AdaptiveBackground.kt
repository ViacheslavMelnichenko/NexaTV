// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.ui.home

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nexatv.launcher.data.BackgroundSettings
import kotlinx.coroutines.CancellationException

/** At most two displayed images; only the incoming image fades, over an opaque outgoing image. */
@Composable
internal fun AdaptiveBackground(settings: BackgroundSettings, scene: BackgroundScene) {
    val assets = LocalContext.current.applicationContext.assets
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val visible = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    var current by remember { mutableStateOf<ImageBitmap?>(null) }
    var previous by remember { mutableStateOf<ImageBitmap?>(null) }
    val opacity = remember { Animatable(1f) }

    LaunchedEffect(scene, settings.blur, visible) {
        previous = null
        opacity.snapTo(1f)
        if (!visible) {
            current = null
            return@LaunchedEffect
        }
        val next = try {
            loadBackground(assets, scene.asset, settings.blur)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: java.io.IOException) {
            // Keep the current image or the underlying theme if an asset cannot be decoded.
            return@LaunchedEffect
        }
        previous = current
        current = next
        if (previous != null && ValueAnimator.areAnimatorsEnabled()) {
            opacity.snapTo(0f)
            opacity.animateTo(1f, tween(durationMillis = 4_000))
        }
        previous = null
    }

    Box(Modifier.fillMaxSize()) {
        previous?.let { BackgroundImage(it, Modifier.fillMaxSize()) }
        current?.let {
            BackgroundImage(it, Modifier.fillMaxSize().graphicsLayer {
                alpha = opacity.value
                // This layer contains one image; modulate it directly without a full-screen offscreen buffer.
                compositingStrategy = CompositingStrategy.ModulateAlpha
            })
        }
        if (current != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = settings.dimPercent / 100f)))
            val vignette = remember {
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent,
                    Color.Black.copy(alpha = 0.28f)))
            }
            Box(Modifier.fillMaxSize().background(vignette))
            if (settings.motion && visible && ValueAnimator.areAnimatorsEnabled()) {
                WeatherParticles(scene.weather)
            }
        }
    }
}

@Composable
private fun BackgroundImage(bitmap: ImageBitmap, modifier: Modifier) {
    Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}
