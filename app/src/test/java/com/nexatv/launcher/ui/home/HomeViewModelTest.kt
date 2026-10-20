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

import android.content.ComponentName
import com.nexatv.launcher.data.AppEntry
import com.nexatv.launcher.data.FavoriteKey
import com.nexatv.launcher.data.FavoritesRepository
import com.nexatv.launcher.data.HomeRoleRepository
import com.nexatv.launcher.data.InstalledAppsRepository
import com.nexatv.launcher.data.SettingsRepository
import com.nexatv.launcher.data.TileSize
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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
    private val favorites = mockk<FavoritesRepository>(relaxUnitFun = true)
    private val settings = mockk<SettingsRepository>(relaxUnitFun = true)

    @Before
    fun setUp() {
        // Default: no live app-change signals, no favorites, tile size stays at the MEDIUM default.
        // Tests override these as needed.
        every { repository.appChanges() } returns emptyFlow()
        every { favorites.favorites } returns emptyFlow()
        every { settings.tileSize } returns emptyFlow()
    }

    @Test
    fun `not effectively Home yields NeedsHomeRole state and skips discovery`() = runTest {
        every { homeRole.isEffectivelyHome() } returns false

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
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

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is HomeUiState.Apps)
    }

    @Test
    fun `empty discovery yields Empty state`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is HomeUiState.Empty)
    }

    @Test
    fun `non-empty discovery yields Apps state with the loaded list`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        val apps = listOf(mockk<AppEntry>(), mockk<AppEntry>())
        coEvery { repository.loadApps() } returns apps

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is HomeUiState.Apps)
        assertEquals(apps, (state as HomeUiState.Apps).apps)
    }

    @Test
    fun `initial state before dispatcher runs is Loading`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)

        assertTrue(viewModel.state.value is HomeUiState.Loading)
    }

    @Test
    fun `an app change reloads the grid`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        every { repository.appChanges() } returns changes
        // First load (init) is empty; after a change signal the second load has an app.
        val apps = listOf(mockk<AppEntry>())
        coEvery { repository.loadApps() } returnsMany listOf(emptyList(), apps)

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is HomeUiState.Empty)

        changes.emit(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is HomeUiState.Apps)
    }

    @Test
    fun `launch delegates to repository and returns true on success`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()
        val entry = mockk<AppEntry>()
        every { repository.launch(entry) } returns true

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.launch(entry))
    }

    @Test
    fun `launch delegates to repository and returns false on failure`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()
        val entry = mockk<AppEntry>()
        every { repository.launch(entry) } returns false

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertFalse(viewModel.launch(entry))
    }

    @Test
    fun `openAppInfo delegates to the repository`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()
        val entry = mockk<AppEntry>()
        every { repository.openAppInfo(entry) } returns true

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.openAppInfo(entry))
    }

    @Test
    fun `toggleFavorite delegates the app key to the repository`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()
        val entry = stubbedApp("pkg/.A", serial = 0L)
        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        viewModel.toggleFavorite(entry)
        advanceUntilIdle()

        coVerify { favorites.toggle(FavoriteKey("pkg/.A", 0L)) }
    }

    @Test
    fun `moveFavorite delegates the indices to the repository`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns emptyList()
        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        viewModel.moveFavorite(from = 2, to = 0)
        advanceUntilIdle()

        coVerify { favorites.reorder(2, 0) }
    }

    @Test
    fun `a pinned favorite is resolved into the favorites row`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        val app = stubbedApp("pkg/.A", serial = 0L)
        coEvery { repository.loadApps() } returns listOf(app)
        val favFlow = MutableSharedFlow<List<FavoriteKey>>(extraBufferCapacity = 1)
        every { favorites.favorites } returns favFlow

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()
        assertTrue((viewModel.state.value as HomeUiState.Apps).favorites.isEmpty())

        favFlow.emit(listOf(FavoriteKey("pkg/.A", 0L)))
        advanceUntilIdle()

        val state = viewModel.state.value as HomeUiState.Apps
        assertEquals(listOf(app), state.favorites)
        assertEquals(listOf(app), state.apps)
    }

    @Test
    fun `tile size defaults to MEDIUM when the store has not emitted`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns listOf(mockk<AppEntry>())

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertEquals(TileSize.MEDIUM, (viewModel.state.value as HomeUiState.Apps).tileSize)
    }

    @Test
    fun `the persisted tile size is reflected in the Apps state`() = runTest {
        every { homeRole.isEffectivelyHome() } returns true
        coEvery { repository.loadApps() } returns listOf(mockk<AppEntry>())
        every { settings.tileSize } returns flowOf(TileSize.LARGE)

        val viewModel = HomeViewModel(repository, homeRole, favorites, settings, mainDispatcherRule.dispatcher)
        advanceUntilIdle()

        assertEquals(TileSize.LARGE, (viewModel.state.value as HomeUiState.Apps).tileSize)
    }

    /** A mock [AppEntry] whose component/serial resolve to a stable [FavoriteKey]. */
    private fun stubbedApp(component: String, serial: Long): AppEntry {
        val componentName = mockk<ComponentName>()
        every { componentName.flattenToString() } returns component
        val entry = mockk<AppEntry>()
        every { entry.component } returns componentName
        every { entry.userSerial } returns serial
        return entry
    }

    /**
     * Swaps the Main dispatcher for a [StandardTestDispatcher] so `viewModelScope` work is
     * controllable from tests, restoring the real dispatcher afterwards.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    class MainDispatcherRule(
        val dispatcher: TestDispatcher = StandardTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
