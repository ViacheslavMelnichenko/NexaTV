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

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Produces the Baseline Profile for NexaTV (NFR-001 cold-start). Running this on a connected TCL
 * writes `app/src/main/baseline-prof.txt`, which the baselineprofile plugin then bundles so the hot
 * startup path is AOT-compiled on first run instead of JIT-warmed.
 *
 * Device-gated: run with `:app:generateBaselineProfile` against the real device. The journey below
 * is the critical first-frame path — cold start to the Home grid; extend it as new screens land.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = LAUNCHER_PACKAGE,
        // The startup path is the one that most benefits from AOT compilation (NFR-001).
        includeInStartupProfile = true,
    ) {
        pressHome()
        launchHome()
    }
}
