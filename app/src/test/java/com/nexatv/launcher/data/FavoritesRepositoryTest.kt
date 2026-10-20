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
import com.nexatv.launcher.data.proto.AppKeyProto
import com.nexatv.launcher.data.proto.LauncherStateProto
import com.nexatv.launcher.data.proto.LauncherStateSerializer
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
 * Unit tests for the favorites persistence layer (ADR-0008): the pure favorite algebra, the Proto
 * DataStore serializer round-trip, and [FavoritesRepository] over a fake in-memory DataStore. All
 * JVM — no device, no Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepositoryTest {

    private val a = FavoriteKey("pkg/.A", userSerial = 0)
    private val b = FavoriteKey("pkg/.B", userSerial = 0)
    private val c = FavoriteKey("pkg/.C", userSerial = 0)

    @Test
    fun `toggle appends an absent key preserving order`() {
        assertEquals(listOf(a, b), toggleFavorite(listOf(a), b))
    }

    @Test
    fun `toggle removes a present key`() {
        assertEquals(listOf(a, c), toggleFavorite(listOf(a, b, c), b))
    }

    @Test
    fun `move reorders within range`() {
        assertEquals(listOf(b, c, a), moveFavorite(listOf(a, b, c), from = 0, to = 2))
    }

    @Test
    fun `move is a no-op for an out-of-range or equal index`() {
        assertEquals(listOf(a, b), moveFavorite(listOf(a, b), from = 0, to = 5))
        assertEquals(listOf(a, b), moveFavorite(listOf(a, b), from = 1, to = 1))
    }

    @Test
    fun `serializer round-trips favorites`() = runTest {
        val state = LauncherStateProto.newBuilder()
            .addFavorites(AppKeyProto.newBuilder().setComponent("pkg/.A").setUserSerial(7))
            .build()

        val bytes = ByteArrayOutputStream().also { LauncherStateSerializer.writeTo(state, it) }.toByteArray()
        val restored = LauncherStateSerializer.readFrom(ByteArrayInputStream(bytes))

        assertEquals(state, restored)
    }

    @Test
    fun `serializer default value is empty`() {
        assertEquals(0, LauncherStateSerializer.defaultValue.favoritesCount)
    }

    @Test
    fun `toggle then reorder persists through the store and the flow reflects it`() = runTest {
        val store = FakeLauncherStore()
        val repository = FavoritesRepository(store)

        repository.toggle(a)
        repository.toggle(b)
        assertEquals(listOf(a, b), repository.favorites.first())

        repository.reorder(from = 0, to = 1)
        assertEquals(listOf(b, a), repository.favorites.first())

        repository.toggle(b) // unpin
        assertEquals(listOf(a), repository.favorites.first())
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
