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
import android.provider.Settings

/**
 * Reads and requests the TV HOME role (ADR-0006). The system — never this app — changes the default
 * Home, so becoming (or ceasing to be) Home is always user-consented and reversible. Full app
 * visibility is coupled to holding this role (ADR-0004), so [isDefaultHome] gates the pre-Home prompt.
 */
class HomeRoleRepository(context: Context) {

    private val appContext = context.applicationContext
    private val roleManager = appContext.getSystemService(RoleManager::class.java)

    /** True when NexaTV currently holds `ROLE_HOME` (i.e. is the default Home app). */
    fun isDefaultHome(): Boolean =
        roleManager?.let { it.isRoleAvailable(RoleManager.ROLE_HOME) && it.isRoleHeld(RoleManager.ROLE_HOME) }
            ?: false

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
