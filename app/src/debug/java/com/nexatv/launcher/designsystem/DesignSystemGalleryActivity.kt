/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.designsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nexatv.launcher.ui.components.AppTile
import com.nexatv.launcher.ui.components.SectionHeader
import com.nexatv.launcher.ui.theme.NexaColorScheme
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * Debug-only living catalogue of the NexaTV design system. Every token and component renders
 * here so visual regressions are caught by eye on a real TV. Ships ONLY in the debug variant
 * (this file lives in `src/debug`), never in release.
 */
class DesignSystemGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexaTheme {
                DesignSystemGallery()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DesignSystemGallery() {
    val colors = NexaTheme.colors
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(NexaTheme.spacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.lg),
        ) {
            Text(text = "NexaTV — Design System", style = MaterialTheme.typography.displayMedium)

            SectionHeader(title = "Color roles")
            ColorSwatches(colors)

            SectionHeader(title = "Typography")
            TypeSamples()

            SectionHeader(title = "Components — AppTile (focus a tile with the D-pad)")
            Row(horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md)) {
                AppTile(label = "Live TV", modifier = Modifier.size(width = 220.dp, height = 132.dp))
                AppTile(label = "Media", modifier = Modifier.size(width = 220.dp, height = 132.dp))
                AppTile(label = "Settings", modifier = Modifier.size(width = 220.dp, height = 132.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ColorSwatches(colors: NexaColorScheme) {
    val roles = listOf(
        "surface" to colors.surface,
        "surfaceVariant" to colors.surfaceVariant,
        "onSurface" to colors.onSurface,
        "onSurfaceMuted" to colors.onSurfaceMuted,
        "accent" to colors.accent,
        "focusRing" to colors.focusRing,
        "success" to colors.success,
        "error" to colors.error,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(NexaTheme.spacing.md)) {
        roles.forEach { (name, value) ->
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Swatch(value)
                Text(text = name, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceMuted)
            }
        }
    }
}

@Composable
private fun Swatch(value: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(NexaTheme.radius.sm))
            .background(value),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TypeSamples() {
    Column(verticalArrangement = Arrangement.spacedBy(NexaTheme.spacing.xs)) {
        Text(text = "displayLarge", style = MaterialTheme.typography.displayLarge)
        Text(text = "headlineMedium", style = MaterialTheme.typography.headlineMedium)
        Text(text = "titleMedium", style = MaterialTheme.typography.titleMedium)
        Text(text = "bodyLarge — primary reading size (>= 18sp)", style = MaterialTheme.typography.bodyLarge)
        Text(text = "bodyMedium — secondary", style = MaterialTheme.typography.bodyMedium)
        Text(text = "labelLarge — captions", style = MaterialTheme.typography.labelLarge)
    }
}
