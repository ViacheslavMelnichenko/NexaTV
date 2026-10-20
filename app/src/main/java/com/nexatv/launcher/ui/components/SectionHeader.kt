/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexatv.launcher.ui.theme.NexaTheme

/**
 * A row/section title. Reads its color and spacing from the active [NexaTheme]. An optional [trailing]
 * (e.g. the Settings gear) is pinned to the row's right edge and vertically centred with the title, so a
 * control can share the header line instead of claiming a whole row of its own above it.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NexaTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = NexaTheme.colors.onSurface,
        )
        trailing?.invoke()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun SectionHeaderPreview() {
    NexaTheme {
        SectionHeader(title = "Applications")
    }
}
