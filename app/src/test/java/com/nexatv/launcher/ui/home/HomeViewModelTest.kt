/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.ui.home

import com.nexatv.launcher.data.AppEntry
import com.nexatv.launcher.data.HomeRoleRepository
import com.nexatv.launcher.data.InstalledAppsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Unit tests for [HomeViewModel]'s state machine. The repository is mocked so discovery runs
 * synchronously under a test dispatcher, with no Android device or framework dependency.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<InstalledAppsRepository>()
    private val homeRole = mockk<HomeRoleRepository>()

    @Test
    fun `not effectively Home yields NeedsHomeRole state and skips discovery`() = runTest {
        every { homeRole.isEffectivelyHome() } returns false

        val viewModel = HomeViewModel(repository, homeRole)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is HomeUiState.NeedsHomeRole)
    }

    @Test
    fun `effectively Home via override yields the app grid`() = runTest {
        // isEffectivelyHome() is true because the HOME-override is enabled (ADR-0007), not the role —
        // the ViewModel treats both the same: it shows the grid, never the prompt.
        every { homeRole.isEffectivelyHome() } returns true
        val apps = listOf(mockk<AppEntry>())
        coEvery { repository.loadApps() } returns apps

        val viewModel = HomeViewModel(repository, homeRole)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is HomeUiState.Apps)
    }

    @Test
    fun `empty discovery yields Empty state`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()

        val viewModel = HomeViewModel(repository, homeRole)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is HomeUiState.Empty)
    }

    @Test
    fun `non-empty discovery yields Apps state with the loaded list`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        val apps = listOf(mockk<AppEntry>(), mockk<AppEntry>())
        coEvery { repository.loadApps() } returns apps

        val viewModel = HomeViewModel(repository, homeRole)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is HomeUiState.Apps)
        assertEquals(apps, (state as HomeUiState.Apps).apps)
    }

    @Test
    fun `initial state before dispatcher runs is Loading`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()

        val viewModel = HomeViewModel(repository, homeRole)

        assertTrue(viewModel.state.value is HomeUiState.Loading)
    }

    @Test
    fun `launch delegates to repository and returns true on success`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()
        val entry = mockk<AppEntry>()
        every { repository.launch(entry) } returns true

        val viewModel = HomeViewModel(repository, homeRole)
        advanceUntilIdle()

        assertTrue(viewModel.launch(entry))
    }

    @Test
    fun `launch delegates to repository and returns false on failure`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()
        val entry = mockk<AppEntry>()
        every { repository.launch(entry) } returns false

        val viewModel = HomeViewModel(repository, homeRole)
        advanceUntilIdle()

        assertFalse(viewModel.launch(entry))
    }

    /**
     * Swaps the Main dispatcher for a [StandardTestDispatcher] so `viewModelScope` work is
     * controllable from tests, restoring the real dispatcher afterwards.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    class MainDispatcherRule(
        private val dispatcher: TestDispatcher = StandardTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
