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

import android.content.ComponentName
import android.os.UserHandle
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for the pure [orderInstalledApps] seam. Android-typed fields ([ComponentName],
 * [UserHandle]) are mocked; component identity relies on mock referential equality, which satisfies
 * the `distinctBy { it.component }` collapse.
 */
class InstalledAppsRepositoryTest {

    private val user = mockk<UserHandle>(relaxed = true)

    private fun entry(
        label: String,
        pkg: String,
        comp: ComponentName = mockk(),
    ): AppEntry = AppEntry(
        label = label,
        packageName = pkg,
        component = comp,
        user = user,
        icon = null,
    )

    @Test
    fun `self package is removed`() {
        val apps = listOf(
            entry("Alpha", "com.other.one"),
            entry("Beta", "com.nexatv.launcher"),
        )

        val result = orderInstalledApps(apps, selfPackage = "com.nexatv.launcher")

        assertEquals(listOf("com.other.one"), result.map { it.packageName })
    }

    @Test
    fun `duplicate components are collapsed to one`() {
        val shared = mockk<ComponentName>()
        val apps = listOf(
            entry("Alpha", "com.dup.app", shared),
            entry("Alpha", "com.dup.app", shared),
        )

        val result = orderInstalledApps(apps, selfPackage = "com.nexatv.launcher")

        assertEquals(1, result.size)
    }

    @Test
    fun `result is sorted case-insensitively by label`() {
        val apps = listOf(
            entry("banana", "com.b"),
            entry("Apple", "com.a"),
            entry("cherry", "com.c"),
        )

        val result = orderInstalledApps(apps, selfPackage = "com.nexatv.launcher")

        assertEquals(listOf("Apple", "banana", "cherry"), result.map { it.label })
    }

    @Test
    fun `empty input yields empty output`() {
        val result = orderInstalledApps(emptyList(), selfPackage = "com.nexatv.launcher")

        assertEquals(emptyList<String>(), result.map { it.label })
    }
}
