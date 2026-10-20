/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.data

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * One launchable application, resolved from [android.content.pm.LauncherApps] (ADR-0004).
 * [component] + [user] together uniquely identify the activity to start (profile-aware).
 * [icon] is pre-rasterized off the UI thread (NFR-003); null when no icon could be loaded
 * (the UI then falls back to a monogram — FR-013).
 */
@Immutable
data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap?,
)
