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

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-gated performance numbers for NexaTV. Two kinds of measurement:
 *  - cold-start time (NFR-001), with and without the Baseline Profile so the before/after is visible;
 *  - frame timing while traversing the Home grid with the D-pad (janky frames are what the eye sees).
 *
 * Run on a connected TCL: `:benchmark:connectedBenchmarkAndroidTest`. These are not unit tests and
 * never run in the JVM/CI lane.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Worst case: no AOT compilation, so it reads as the floor the Baseline Profile has to beat. */
    @Test
    fun coldStartupNoCompilation() = measureStartup(CompilationMode.None())

    /** With the generated Baseline Profile applied — the number users actually get after first run. */
    @Test
    fun coldStartupBaselineProfile() =
        measureStartup(CompilationMode.Partial())

    private fun measureStartup(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = LAUNCHER_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        compilationMode = compilationMode,
        // NexaTV is the active HOME; the OS resurrects it after the COLD kill, leaving nothing to
        // transition to. Surface Settings first (not timed) so the measured launchHome() is a real,
        // confirmable foreground transition. See LauncherTarget.leaveHome.
        setupBlock = { leaveHome() },
    ) {
        launchHome()
    }

    /** Frame timing as focus moves across the grid — the D-pad is the only input on a TV remote. */
    @Test
    fun gridTraversalFrameTiming() = rule.measureRepeated(
        packageName = LAUNCHER_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        // Same HOME caveat: leave Home so launchHome() actually brings the grid to the foreground and
        // produces frames; otherwise re-launching the already-visible HOME draws nothing to measure.
        setupBlock = {
            leaveHome()
            launchHome()
        },
    ) {
        repeat(DPAD_STEPS) {
            device.pressDPadRight()
            device.waitForIdle()
        }
    }

    private companion object {
        const val ITERATIONS = 10
        const val DPAD_STEPS = 8
    }
}
