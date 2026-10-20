/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.benchmark

import android.content.Intent
import android.provider.Settings
import androidx.benchmark.macro.MacrobenchmarkScope

/** The release application id of the launcher under test (NOT the .debug suffix). */
const val LAUNCHER_PACKAGE = "com.nexatv.launcher"

private const val LAUNCHER_ACTIVITY = "com.nexatv.launcher.MainActivity"

/**
 * Cold-start the launcher by its explicit component. NexaTV registers only HOME / LEANBACK_LAUNCHER
 * intent-filters (no plain category LAUNCHER), so `getLaunchIntentForPackage` can return null on this
 * firmware — we name the activity directly instead of relying on the default launch intent.
 */
internal fun MacrobenchmarkScope.launchHome() {
    startActivityAndWait(
        Intent().apply {
            setClassName(LAUNCHER_PACKAGE, LAUNCHER_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

/**
 * Move the launcher off the foreground before a measured launch. NexaTV is the active HOME, which the
 * OS immediately resurrects after `killProcess` — so a plain cold launch has nothing to transition to
 * and Macrobenchmark cannot confirm it ("Unable to confirm activity launch completion []"). Surfacing
 * system Settings first makes the following [launchHome] a genuine foreground transition the harness
 * can observe. Settings is guaranteed present on Android TV and is read-only/reversible (BACK returns).
 */
internal fun MacrobenchmarkScope.leaveHome() {
    startActivityAndWait(
        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
