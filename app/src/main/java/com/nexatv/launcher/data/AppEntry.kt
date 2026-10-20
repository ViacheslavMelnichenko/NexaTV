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
 * [userSerial] is the profile's stable serial (UserManager) — the persistence-safe form of [user]
 * used to key favorites (ADR-0008), since a raw [UserHandle] is not durable across reboots.
 * [icon] is pre-rasterized off the UI thread (NFR-003); null when no icon could be loaded
 * (the UI then falls back to a monogram — FR-013).
 * [banner] is the app's wide 16:9 TV banner (the branded art Google TV shows), when it ships one;
 * the tile prefers it and falls back to [icon] on a card. Also pre-rasterized; null for apps that
 * declare no `android:banner` (most side-loaded phone apps).
 */
@Immutable
data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val userSerial: Long,
    val icon: ImageBitmap?,
    val banner: ImageBitmap? = null,
)
