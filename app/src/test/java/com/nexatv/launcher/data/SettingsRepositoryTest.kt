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

import androidx.datastore.core.DataStore
import com.nexatv.launcher.data.proto.LauncherStateProto
import com.nexatv.launcher.data.proto.LauncherStateSerializer
import com.nexatv.launcher.data.proto.ThemeId as ThemeIdProto
import com.nexatv.launcher.data.proto.TileSize as TileSizeProto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Unit tests for the settings persistence layer (ADR-0009): the proto<->domain tile-size mapping,
 * the default-on-unset behaviour, [SettingsRepository] over a fake in-memory DataStore, and that a
 * settings write survives the serializer round-trip alongside favorites. All JVM — no device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @Test
    fun `tile size maps across the domain-proto boundary both ways`() {
        for (size in TileSize.entries) {
            assertEquals(size, size.toProto().toDomain())
        }
        assertEquals(TileSizeProto.TILE_SIZE_SMALL, TileSize.SMALL.toProto())
        assertEquals(TileSizeProto.TILE_SIZE_MEDIUM, TileSize.MEDIUM.toProto())
        assertEquals(TileSizeProto.TILE_SIZE_LARGE, TileSize.LARGE.toProto())
    }

    @Test
    fun `unspecified and unknown tile sizes resolve to medium`() {
        assertEquals(TileSize.MEDIUM, TileSizeProto.TILE_SIZE_UNSPECIFIED.toDomain())
        assertEquals(TileSize.MEDIUM, TileSizeProto.UNRECOGNIZED.toDomain())
    }

    @Test
    fun `tile size defaults to medium on an unset store`() = runTest {
        val repository = SettingsRepository(FakeLauncherStore())
        assertEquals(TileSize.MEDIUM, repository.tileSize.first())
    }

    @Test
    fun `setTileSize persists through the store and the flow reflects it`() = runTest {
        val repository = SettingsRepository(FakeLauncherStore())

        repository.setTileSize(TileSize.SMALL)
        assertEquals(TileSize.SMALL, repository.tileSize.first())

        repository.setTileSize(TileSize.LARGE)
        assertEquals(TileSize.LARGE, repository.tileSize.first())
    }

    @Test
    fun `setTileSize leaves favorites untouched`() = runTest {
        val store = FakeLauncherStore()
        val favorites = FavoritesRepository(store)
        val settings = SettingsRepository(store)
        val key = FavoriteKey("pkg/.A", userSerial = 0)

        favorites.toggle(key)
        settings.setTileSize(TileSize.LARGE)

        assertEquals(listOf(key), favorites.favorites.first())
        assertEquals(TileSize.LARGE, settings.tileSize.first())
    }

    @Test
    fun `theme maps across the domain-proto boundary both ways`() {
        for (choice in ThemeChoice.entries) {
            assertEquals(choice, choice.toProto().toDomain())
        }
        assertEquals(ThemeIdProto.THEME_ID_PASTEL_DARK, ThemeChoice.PASTEL_DARK.toProto())
        assertEquals(ThemeIdProto.THEME_ID_OLED_BLACK, ThemeChoice.OLED_BLACK.toProto())
        assertEquals(ThemeIdProto.THEME_ID_AMBER_EMBER, ThemeChoice.AMBER_EMBER.toProto())
        assertEquals(ThemeIdProto.THEME_ID_MAGENTA_NOIR, ThemeChoice.MAGENTA_NOIR.toProto())
    }

    @Test
    fun `unspecified and unknown themes resolve to the default`() {
        assertEquals(ThemeChoice.Default, ThemeIdProto.THEME_ID_UNSPECIFIED.toDomain())
        assertEquals(ThemeChoice.Default, ThemeIdProto.UNRECOGNIZED.toDomain())
    }

    @Test
    fun `theme defaults to Pastel Dark on an unset store`() = runTest {
        val repository = SettingsRepository(FakeLauncherStore())
        assertEquals(ThemeChoice.PASTEL_DARK, repository.theme.first())
    }

    @Test
    fun `setTheme persists through the store and the flow reflects it`() = runTest {
        val repository = SettingsRepository(FakeLauncherStore())

        repository.setTheme(ThemeChoice.AMBER_EMBER)
        assertEquals(ThemeChoice.AMBER_EMBER, repository.theme.first())

        repository.setTheme(ThemeChoice.MAGENTA_NOIR)
        assertEquals(ThemeChoice.MAGENTA_NOIR, repository.theme.first())
    }

    @Test
    fun `setTheme leaves tile size and favorites untouched`() = runTest {
        val store = FakeLauncherStore()
        val favorites = FavoritesRepository(store)
        val settings = SettingsRepository(store)
        val key = FavoriteKey("pkg/.A", userSerial = 0)

        favorites.toggle(key)
        settings.setTileSize(TileSize.LARGE)
        settings.setTheme(ThemeChoice.OLED_BLACK)

        assertEquals(listOf(key), favorites.favorites.first())
        assertEquals(TileSize.LARGE, settings.tileSize.first())
        assertEquals(ThemeChoice.OLED_BLACK, settings.theme.first())
    }

    @Test
    fun `serializer round-trips a settings tile size`() = runTest {
        val state = LauncherStateProto.newBuilder()
            .setSettings(
                LauncherStateProto.getDefaultInstance().settings.toBuilder()
                    .setTileSize(TileSizeProto.TILE_SIZE_LARGE),
            )
            .build()

        val bytes = ByteArrayOutputStream().also { LauncherStateSerializer.writeTo(state, it) }.toByteArray()
        val restored = LauncherStateSerializer.readFrom(ByteArrayInputStream(bytes))

        assertEquals(state, restored)
        assertEquals(TileSizeProto.TILE_SIZE_LARGE, restored.settings.tileSize)
    }

    @Test
    fun `old stores keep theme background while preview can opt into adaptive`() = runTest {
        val store = FakeLauncherStore()
        assertEquals(BackgroundMode.THEME, SettingsRepository(store).background.first().mode)
        val preview = SettingsRepository(store, BackgroundSettings(mode = BackgroundMode.ADAPTIVE))
        assertEquals(BackgroundMode.ADAPTIVE, preview.background.first().mode)
        preview.setBackground(BackgroundSettings())
        assertEquals(BackgroundMode.THEME, preview.background.first().mode)
    }

    @Test
    fun `background survives serialization without changing favorites or theme`() = runTest {
        val store = FakeLauncherStore()
        val settings = SettingsRepository(store)
        val favorites = FavoritesRepository(store)
        val key = FavoriteKey("pkg/.A", 0)
        favorites.toggle(key)
        settings.setTheme(ThemeChoice.OLED_BLACK)
        val background = BackgroundSettings(BackgroundMode.MANUAL, DayPeriod.NIGHT, SceneWeather.SNOW,
            BackgroundBlur.STRONG, 40, true)
        settings.setBackground(background)
        val bytes = ByteArrayOutputStream().also { LauncherStateSerializer.writeTo(store.data.first(), it) }
        val restored = FakeLauncherStore(LauncherStateSerializer.readFrom(ByteArrayInputStream(bytes.toByteArray())))
        assertEquals(background, SettingsRepository(restored).background.first())
        assertEquals(ThemeChoice.OLED_BLACK, SettingsRepository(restored).theme.first())
        assertEquals(listOf(key), FavoritesRepository(restored).favorites.first())
    }

    @Test
    fun `unknown background values fall back and dimming is bounded`() {
        val unknown = com.nexatv.launcher.data.proto.BackgroundSettingsProto.newBuilder()
            .setMode("FUTURE_MODE").setPeriod("UNKNOWN").setBlur("UNKNOWN").setDimPercent(999).build()
        val settings = unknown.toBackgroundSettings()
        assertEquals(BackgroundMode.THEME, settings.mode)
        assertEquals(DayPeriod.DAY, settings.period)
        assertEquals(BackgroundBlur.SOFT, settings.blur)
        assertEquals(70, settings.dimPercent)
    }

    /** Minimal in-memory [DataStore] backed by a [MutableStateFlow] — no file, no Android. */
    private class FakeLauncherStore(
        initial: LauncherStateProto = LauncherStateProto.getDefaultInstance(),
    ) : DataStore<LauncherStateProto> {
        private val flow = MutableStateFlow(initial)
        override val data: Flow<LauncherStateProto> = flow
        override suspend fun updateData(
            transform: suspend (t: LauncherStateProto) -> LauncherStateProto,
        ): LauncherStateProto = transform(flow.value).also { flow.value = it }
    }
}
