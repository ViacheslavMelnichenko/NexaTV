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
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Enumerates and launches installed apps via [LauncherApps] (ADR-0004) — no `QUERY_ALL_PACKAGES`.
 * Full visibility depends on NexaTV holding `ROLE_HOME` (M3 / FR-015); until then the list may be
 * partial, which the UI surfaces as an empty/limited state rather than an error.
 */
class InstalledAppsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps =
        appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = appContext.getSystemService(UserManager::class.java)

    /** Package to hide from its own grid (debug builds carry the `.debug` suffix). */
    private val selfPackage = appContext.packageName

    /**
     * Load launchable apps across every current-user profile, de-duplicated and sorted by label.
     * Icons are rasterized here (off the UI thread) so composition stays cheap (NFR-003).
     */
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.Default) {
        val rawList = launcherApps.profiles
            .flatMap { user -> runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList()) }
            .map { it.toEntry() }
        orderInstalledApps(rawList, selfPackage)
    }

    /**
     * Cold stream that emits once per install/removal/change of a launchable app (FR-002). Backed by a
     * [LauncherApps.Callback] registered while collected and unregistered on cancellation, so there is no
     * leak and no cost when nothing observes it. Emits bare signals (not deltas): the collector re-queries
     * [loadApps], which is the single source of truth and already de-dups/sorts.
     */
    fun appChanges(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackageChanged(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackagesAvailable(names: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(Unit)
            }
            override fun onPackagesUnavailable(names: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback)
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    /** Start an app's main activity. Returns false on failure (FR-003 keeps focus, shows a message). */
    fun launch(entry: AppEntry): Boolean = runCatching {
        launcherApps.startMainActivity(entry.component, entry.user, null, null)
    }.isSuccess

    /** Open the system "App info" screen for [entry] (the context-menu "Інформація" action). */
    fun openAppInfo(entry: AppEntry): Boolean = runCatching {
        launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null)
    }.isSuccess

    private fun LauncherActivityInfo.toEntry(): AppEntry {
        val icon = runCatching {
            getBadgedIcon(0).toBitmap(width = ICON_PX, height = ICON_PX).asImageBitmap()
        }.getOrNull()
        return AppEntry(
            label = label?.toString().orEmpty(),
            packageName = applicationInfo.packageName,
            component = componentName,
            user = user,
            userSerial = userManager?.getSerialNumberForUser(user) ?: 0L,
            icon = icon,
        )
    }

    private companion object {
        /** Downsample icons to a bounded size (FR-013 memory-bounded art, NFR-002). */
        const val ICON_PX = 128
    }
}

/**
 * Pure ordering seam for the installed-apps list: drops [selfPackage], de-duplicates by
 * [AppEntry.component], and sorts case-insensitively by [AppEntry.label]. Extracted from
 * [InstalledAppsRepository.loadApps] so the (Android-free) semantics are unit-testable.
 */
internal fun orderInstalledApps(apps: List<AppEntry>, selfPackage: String): List<AppEntry> =
    apps.asSequence()
        .filter { it.packageName != selfPackage }
        .distinctBy { it.component }
        .sortedBy { it.label.lowercase() }
        .toList()
