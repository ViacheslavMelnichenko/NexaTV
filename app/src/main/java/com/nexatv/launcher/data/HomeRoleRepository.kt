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

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Reads and requests the TV HOME role (ADR-0006). The system — never this app — changes the default
 * Home, so becoming (or ceasing to be) Home is always user-consented and reversible. Full app
 * visibility is coupled to being Home (ADR-0004), so [isDefaultHome] gates the pre-Home prompt.
 */
class HomeRoleRepository(context: Context) {

    private val appContext = context.applicationContext
    private val roleManager = appContext.getSystemService(RoleManager::class.java)

    /**
     * True when NexaTV is the *effective* default Home — i.e. what the HOME button actually opens.
     *
     * We resolve the HOME intent via [PackageManager] rather than `RoleManager.isRoleHeld`: on this
     * TCL firmware `isRoleAvailable(ROLE_HOME)` is false and the role-holder record disagrees with
     * the resolver (validation finding, 2026-10-20), so the role API is an unreliable source of
     * truth. The resolver always reflects real HOME behaviour and is firmware-independent.
     */
    fun isDefaultHome(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = appContext.packageManager
            .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == appContext.packageName
    }

    /**
     * Intent that asks the system to make NexaTV the default Home. Prefer the first-party role request
     * (a system-owned, reversible consent flow); fall back to the Home-settings picker if the role is
     * unavailable on this firmware (ADR-0006).
     */
    fun requestHomeIntent(): Intent =
        roleManager
            ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_HOME) }
            ?.createRequestRoleIntent(RoleManager.ROLE_HOME)
            ?: Intent(Settings.ACTION_HOME_SETTINGS)
}
