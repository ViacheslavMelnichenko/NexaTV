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
