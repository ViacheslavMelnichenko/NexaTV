// Copyright 2026 Viacheslav Melnichenko. Licensed under the Apache License, Version 2.0.
package com.nexatv.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexatv.launcher.data.Weather
import com.nexatv.launcher.data.WeatherRepository
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

internal data class HomeEnvironment(
    val now: Instant = Instant.now(),
    val weather: Weather? = null,
    val loaded: Boolean = false,
)

/** One shared request for both widget and background; no polling while Home has no visible collector. */
internal class HomeEnvironmentViewModel(
    private val fetch: suspend () -> Weather? = { WeatherRepository().fetch() },
    private val clock: () -> Instant = Instant::now,
) : ViewModel() {
    private var cached: Weather? = null
    private var fetchedAt: Instant? = null
    private var attemptedAt: Instant? = null
    private var retrySeconds = 0L

    val state = flow {
        while (true) {
            val now = clock()
            emit(snapshot(now))
            val age = attemptedAt?.let { now.epochSecond - it.epochSecond }
            if (age == null || age < 0 || age >= retrySeconds) {
                val result = fetch()
                attemptedAt = clock()
                if (result != null) {
                    cached = result
                    fetchedAt = clock()
                }
                // Failed requests retry gently; valid observations refresh every half hour.
                retrySeconds = if (result == null) 5 * 60L else 30 * 60L
                emit(snapshot(clock()))
            }
            delay(60_000L)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
        HomeEnvironment(),
    )

    private fun snapshot(now: Instant) = HomeEnvironment(
        now = now,
        weather = freshWeather(cached, fetchedAt, now),
        loaded = attemptedAt != null,
    )
}
