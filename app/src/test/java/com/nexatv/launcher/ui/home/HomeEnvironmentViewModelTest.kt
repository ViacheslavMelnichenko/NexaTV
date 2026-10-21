// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.ui.home

import com.nexatv.launcher.data.Weather
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeEnvironmentViewModelTest {
    @get:Rule val dispatcher = HomeViewModelTest.MainDispatcherRule()

    @Test
    fun `two consumers share requests and hidden Home performs no polling`() = runTest {
        var calls = 0
        val origin = Instant.parse("2026-10-21T10:00:00Z")
        val model = HomeEnvironmentViewModel(
            fetch = { calls++; Weather(12, 8, 15) },
            clock = { origin.plusMillis(testScheduler.currentTime) },
        )
        val first = backgroundScope.launch { model.state.collect {} }
        val second = backgroundScope.launch { model.state.collect {} }
        runCurrent()
        assertEquals(1, calls)
        advanceTimeBy(30 * 60_000L)
        runCurrent()
        assertEquals(2, calls)
        first.cancel()
        second.cancel()
        runCurrent()
        advanceTimeBy(4 * 60 * 60_000L)
        runCurrent()
        assertEquals(2, calls)
        assertNull(model.state.value.weather)
        backgroundScope.launch { model.state.collect {} }
        runCurrent()
        assertEquals(3, calls)
    }

    @Test
    fun `failed refresh retains fresh data then expires it`() = runTest {
        var calls = 0
        val origin = Instant.parse("2026-10-21T10:00:00Z")
        val weather = Weather(12, 8, 15)
        val model = HomeEnvironmentViewModel(
            fetch = { calls++; if (calls == 1) weather else null },
            clock = { origin.plusMillis(testScheduler.currentTime) },
        )
        backgroundScope.launch { model.state.collect {} }
        runCurrent()
        advanceTimeBy(31 * 60_000L)
        runCurrent()
        assertEquals(weather, model.state.value.weather)
        advanceTimeBy(3 * 60 * 60_000L)
        runCurrent()
        assertNull(model.state.value.weather)
    }
}
