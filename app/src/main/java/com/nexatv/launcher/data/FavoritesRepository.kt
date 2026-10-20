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
import androidx.datastore.dataStore
import com.nexatv.launcher.data.proto.AppKeyProto
import com.nexatv.launcher.data.proto.LauncherStateProto
import com.nexatv.launcher.data.proto.LauncherStateSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persisted identity of a favorite app (ADR-0008). Keyed by the flattened component + profile
 * serial so a pinned favorite survives app updates (the component is stable across versions) and
 * stays distinct per work/clone profile. Android-free so the favorite algebra is unit-testable.
 */
data class FavoriteKey(val component: String, val userSerial: Long)

/** The persisted key for a discovered app — component + profile serial (ADR-0008). */
fun AppEntry.toFavoriteKey(): FavoriteKey = FavoriteKey(component.flattenToString(), userSerial)

/**
 * Resolve persisted favorite [keys] against the currently discovered [apps], in favorite order.
 * Keys with no matching installed app (e.g. a favorite that was since uninstalled) are dropped, so
 * the favorites row never shows a dead tile. Pure/Android-free so it is unit-testable.
 */
internal fun resolveFavorites(apps: List<AppEntry>, keys: List<FavoriteKey>): List<AppEntry> {
    if (keys.isEmpty()) return emptyList()
    val byKey = apps.associateBy { it.toFavoriteKey() }
    return keys.mapNotNull { byKey[it] }
}

/**
 * Owns the user's favorites, persisted via Proto DataStore (ADR-0008). Reads are a cold [Flow] off
 * the store (collected on the UI path at cold start, NFR-001/003); writes are transactional through
 * [DataStore.updateData]. The store is injected so this is testable with a fake DataStore, no device.
 */
class FavoritesRepository(private val dataStore: DataStore<LauncherStateProto>) {

    /** The current favorites, in pin / reorder order; emits again on every change. */
    val favorites: Flow<List<FavoriteKey>> =
        dataStore.data.map { state -> state.favoritesList.map { it.toKey() } }

    /** Pin [key] if absent, unpin it if present (appends on pin, keeping order). */
    suspend fun toggle(key: FavoriteKey) {
        dataStore.updateData { state ->
            state.withFavorites(toggleFavorite(state.favoritesList.map { it.toKey() }, key))
        }
    }

    /** Move the favorite at [from] to [to], preserving the rest of the order. No-op if out of range. */
    suspend fun reorder(from: Int, to: Int) {
        dataStore.updateData { state ->
            state.withFavorites(moveFavorite(state.favoritesList.map { it.toKey() }, from, to))
        }
    }
}

private fun LauncherStateProto.withFavorites(keys: List<FavoriteKey>): LauncherStateProto =
    toBuilder().clearFavorites().addAllFavorites(keys.map { it.toProto() }).build()

private fun AppKeyProto.toKey(): FavoriteKey = FavoriteKey(component, userSerial)

private fun FavoriteKey.toProto(): AppKeyProto =
    AppKeyProto.newBuilder().setComponent(component).setUserSerial(userSerial).build()

/**
 * Pure toggle: remove [key] if already present, else append it. Extracted (Android-free) so the
 * pin/unpin semantics are unit-testable without DataStore.
 */
internal fun toggleFavorite(current: List<FavoriteKey>, key: FavoriteKey): List<FavoriteKey> =
    if (key in current) current.filterNot { it == key } else current + key

/**
 * Pure reorder: move the item at [from] to [to], shifting the rest. Returns [current] unchanged if
 * either index is out of range or they are equal. Extracted so reorder semantics are unit-testable.
 */
internal fun moveFavorite(current: List<FavoriteKey>, from: Int, to: Int): List<FavoriteKey> {
    if (from !in current.indices || to !in current.indices || from == to) return current
    return current.toMutableList().apply { add(to, removeAt(from)) }
}

/** App-internal store file (ADR-0008); local only, consistent with no-INTERNET / `allowBackup=false`. */
private val Context.launcherStateStore: DataStore<LauncherStateProto> by dataStore(
    fileName = "launcher_state.pb",
    serializer = LauncherStateSerializer,
)

/** Production factory: a [FavoritesRepository] over the app-internal DataStore. */
fun favoritesRepository(context: Context): FavoritesRepository =
    FavoritesRepository(context.applicationContext.launcherStateStore)
