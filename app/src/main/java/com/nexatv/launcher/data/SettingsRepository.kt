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
import com.nexatv.launcher.BuildConfig
import androidx.datastore.core.DataStore
import com.nexatv.launcher.data.proto.LauncherStateProto
import com.nexatv.launcher.data.proto.ThemeId as ThemeIdProto
import com.nexatv.launcher.data.proto.TileSize as TileSizeProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Home tile-size preset (ADR-0009). A semantic S/M/L token, not raw dp — the UI maps each to actual
 * dimensions, so storage stays layout-free. Android-free so the proto mapping is unit-testable.
 */
enum class TileSize { SMALL, MEDIUM, LARGE }

/**
 * Color-theme choice (Theming EPIC). A semantic id, not raw colors — the ui/theme layer maps each to a
 * [NexaColorScheme][com.nexatv.launcher.ui.theme.NexaColorScheme], so storage stays color-free (ADR-0009).
 * Android-free so the proto mapping is unit-testable. [Default] is the palette used when the owner never
 * chose (and for any unknown/future stored value).
 */
enum class ThemeChoice {
    PASTEL_DARK,
    OLED_BLACK,
    AMBER_EMBER,
    MAGENTA_NOIR,
    ;

    companion object {
        val Default: ThemeChoice = PASTEL_DARK
    }
}

/**
 * Owns user settings, persisted via the shared Proto DataStore (ADR-0008/0009). Mirrors
 * [FavoritesRepository]: reads are a cold [Flow] off the store (collected on the UI path at cold
 * start, NFR-001/003), writes are transactional through [DataStore.updateData]. The store is
 * injected so this is testable with a fake DataStore, no device.
 */
class SettingsRepository(
    private val dataStore: DataStore<LauncherStateProto>,
    private val backgroundDefault: BackgroundSettings = BackgroundSettings(),
) {

    val background: Flow<BackgroundSettings> = dataStore.data.map { state ->
        if (state.settings.hasBackground()) state.settings.background.toBackgroundSettings() else backgroundDefault
    }

    suspend fun setBackground(value: BackgroundSettings) {
        dataStore.updateData { state ->
            state.toBuilder().setSettings(state.settings.toBuilder().setBackground(value.toProto())).build()
        }
    }

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

    /** The chosen color theme; emits again on every change. Defaults to [ThemeChoice.Default]. */
    val theme: Flow<ThemeChoice> =
        dataStore.data.map { it.settings.theme.toDomain() }

    /** Persist the color theme. Writes only the `settings` sub-message, leaving favorites + tile size intact. */
    suspend fun setTheme(choice: ThemeChoice) {
        dataStore.updateData { state ->
            state.toBuilder()
                .setSettings(state.settings.toBuilder().setTheme(choice.toProto()))
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

/**
 * Resolve the persisted proto enum to the domain enum. `THEME_ID_UNSPECIFIED` (owner never chose),
 * an explicit `PASTEL_DARK`, and any unknown/future value all resolve to [ThemeChoice.Default].
 */
internal fun ThemeIdProto.toDomain(): ThemeChoice = when (this) {
    ThemeIdProto.THEME_ID_OLED_BLACK -> ThemeChoice.OLED_BLACK
    ThemeIdProto.THEME_ID_AMBER_EMBER -> ThemeChoice.AMBER_EMBER
    ThemeIdProto.THEME_ID_MAGENTA_NOIR -> ThemeChoice.MAGENTA_NOIR
    else -> ThemeChoice.Default
}

/** Map the domain enum to its persisted proto value. */
internal fun ThemeChoice.toProto(): ThemeIdProto = when (this) {
    ThemeChoice.PASTEL_DARK -> ThemeIdProto.THEME_ID_PASTEL_DARK
    ThemeChoice.OLED_BLACK -> ThemeIdProto.THEME_ID_OLED_BLACK
    ThemeChoice.AMBER_EMBER -> ThemeIdProto.THEME_ID_AMBER_EMBER
    ThemeChoice.MAGENTA_NOIR -> ThemeIdProto.THEME_ID_MAGENTA_NOIR
}

/** Production factory: a [SettingsRepository] over the shared app-internal DataStore. */
fun settingsRepository(context: Context): SettingsRepository =
    SettingsRepository(
        context.applicationContext.launcherStateStore,
        BackgroundSettings(mode = if (BuildConfig.IS_PREVIEW) BackgroundMode.ADAPTIVE else BackgroundMode.THEME),
    )
