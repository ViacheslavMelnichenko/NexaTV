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

import android.content.Context
import androidx.datastore.core.DataStore
import com.nexatv.launcher.data.proto.LauncherStateProto
import com.nexatv.launcher.data.proto.TileSize as TileSizeProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Home tile-size preset (ADR-0009). A semantic S/M/L token, not raw dp — the UI maps each to actual
 * dimensions, so storage stays layout-free. Android-free so the proto mapping is unit-testable.
 */
enum class TileSize { SMALL, MEDIUM, LARGE }

/**
 * Owns user settings, persisted via the shared Proto DataStore (ADR-0008/0009). Mirrors
 * [FavoritesRepository]: reads are a cold [Flow] off the store (collected on the UI path at cold
 * start, NFR-001/003), writes are transactional through [DataStore.updateData]. The store is
 * injected so this is testable with a fake DataStore, no device.
 */
class SettingsRepository(private val dataStore: DataStore<LauncherStateProto>) {

    /** The chosen home tile size; emits again on every change. Defaults to [TileSize.MEDIUM]. */
    val tileSize: Flow<TileSize> =
        dataStore.data.map { it.settings.tileSize.toDomain() }

    /** Persist the home tile size. Writes only the `settings` sub-message, leaving favorites intact. */
    suspend fun setTileSize(size: TileSize) {
        dataStore.updateData { state ->
            state.toBuilder()
                .setSettings(state.settings.toBuilder().setTileSize(size.toProto()))
                .build()
        }
    }
}

/**
 * Resolve the persisted proto enum to the domain enum. `TILE_SIZE_UNSPECIFIED` (owner never chose),
 * an explicit `MEDIUM`, and any unknown/future value all resolve to [TileSize.MEDIUM] — the safe default.
 */
internal fun TileSizeProto.toDomain(): TileSize = when (this) {
    TileSizeProto.TILE_SIZE_SMALL -> TileSize.SMALL
    TileSizeProto.TILE_SIZE_LARGE -> TileSize.LARGE
    else -> TileSize.MEDIUM
}

/** Map the domain enum to its persisted proto value. */
internal fun TileSize.toProto(): TileSizeProto = when (this) {
    TileSize.SMALL -> TileSizeProto.TILE_SIZE_SMALL
    TileSize.MEDIUM -> TileSizeProto.TILE_SIZE_MEDIUM
    TileSize.LARGE -> TileSizeProto.TILE_SIZE_LARGE
}

/** Production factory: a [SettingsRepository] over the shared app-internal DataStore. */
fun settingsRepository(context: Context): SettingsRepository =
    SettingsRepository(context.applicationContext.launcherStateStore)
