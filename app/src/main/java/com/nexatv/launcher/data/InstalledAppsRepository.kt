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
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
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
    private val packageManager = appContext.packageManager

    /** Package to hide from its own grid (debug builds carry the `.debug` suffix). */
    private val selfPackage = appContext.packageName

    /**
     * Load launchable apps across every current-user profile, de-duplicated and sorted by label.
     * Icons are rasterized here (off the UI thread) so composition stays cheap (NFR-003).
     */
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.Default) {
        val launchable = launcherApps.profiles
            .flatMap { user -> runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList()) }
            .map { it.toEntry() }
        // LauncherApps.getActivityList only returns CATEGORY_LAUNCHER activities; TV-native apps
        // (Prime Video, Google TV, Play Games, …) ship a CATEGORY_LEANBACK_LAUNCHER entry *only*, so
        // they are otherwise invisible. Supplement with the leanback launcher set for packages the
        // standard query didn't already cover (FR-002 "see every launchable app").
        val seen = launchable.mapTo(HashSet()) { it.packageName }
        val leanbackOnly = leanbackLauncherEntries().filterNot { it.packageName in seen }
        orderInstalledApps(launchable + leanbackOnly, selfPackage)
    }

    /**
     * The TV (leanback) launcher activities for the current user, as [AppEntry]s. Resolved via
     * [android.content.pm.PackageManager] because [LauncherApps] exposes no leanback-category query;
     * visibility of these packages comes from the `<queries>` launcher intents in the manifest.
     */
    private fun leanbackLauncherEntries(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val resolved = runCatching { packageManager.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
        val user = Process.myUserHandle()
        val serial = userManager?.getSerialNumberForUser(user) ?: 0L
        return resolved.mapNotNull { it.toEntry(user, serial) }
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
    }.recoverCatching {
        // Leanback-only apps aren't always startable via LauncherApps' main-activity path; fall back
        // to their explicit leanback launch intent.
        appContext.startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(entry.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
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
            banner = loadBanner(componentName, applicationInfo.packageName),
        )
    }

    /** Map a leanback [ResolveInfo] to an [AppEntry]; null if it carries no activity. */
    private fun ResolveInfo.toEntry(user: UserHandle, serial: Long): AppEntry? {
        val info = activityInfo ?: return null
        val component = ComponentName(info.packageName, info.name)
        val icon = runCatching {
            loadIcon(packageManager).toBitmap(width = ICON_PX, height = ICON_PX).asImageBitmap()
        }.getOrNull()
        return AppEntry(
            label = loadLabel(packageManager).toString(),
            packageName = info.packageName,
            component = component,
            user = user,
            userSerial = serial,
            icon = icon,
            banner = loadBanner(component, info.packageName),
        )
    }

    /**
     * The app's wide 16:9 TV banner (the branded art shown in Google TV rows), rasterized off the UI
     * thread to a bounded size (NFR-002/NFR-003). Activity banner first, then the application banner;
     * null when the app declares no `android:banner`, which the tile handles by showing the icon on a
     * card. Visibility of these packages comes from the `<queries>` launcher intents in the manifest.
     */
    private fun loadBanner(component: ComponentName, packageName: String): ImageBitmap? = runCatching {
        val banner = packageManager.getActivityBanner(component)
            ?: packageManager.getApplicationBanner(packageName)
        banner?.toBitmap(width = BANNER_W_PX, height = BANNER_H_PX)?.asImageBitmap()
    }.getOrNull()

    private companion object {
        /** Downsample icons to a bounded size (FR-013 memory-bounded art, NFR-002). */
        const val ICON_PX = 128

        /** Bounded 16:9 banner raster (320×180 — the TV banner guideline aspect). */
        const val BANNER_W_PX = 320
        const val BANNER_H_PX = 180
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
