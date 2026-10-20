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

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.nexatv.launcher.override.NexaOverrideService

/**
 * Reads and requests the TV HOME role (ADR-0006) and, on firmware that blocks ROLE_HOME replacement,
 * the HOME-override accessibility service (ADR-0007). The system — never this app — changes the
 * default Home or enables the override, so becoming (or ceasing to be) Home is always user-consented
 * and reversible. Full app visibility is coupled to being Home (ADR-0004), so [isEffectivelyHome]
 * gates the pre-Home prompt.
 */
class HomeRoleRepository(context: Context) {

    private val appContext = context.applicationContext
    private val roleManager = appContext.getSystemService(RoleManager::class.java)
    private val accessibilityManager = appContext.getSystemService(AccessibilityManager::class.java)

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
     * True when the HOME-override accessibility service (ADR-0007) is enabled. On Google TV firmware
     * that blocks ROLE_HOME replacement, this service — not the role — is what actually makes the HOME
     * button open NexaTV (validated on the TCL, 2026-10-20), so once it is on we are effectively Home.
     *
     * Note: the system only binds the service after the owner enables it from Settings → Accessibility
     * (an `adb settings put` enable is ignored on this firmware), so this reflects a real, consented
     * override.
     */
    fun isHomeOverrideEnabled(): Boolean {
        val manager = accessibilityManager ?: return false
        val self = ComponentName(appContext, NexaOverrideService::class.java)
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val service = info.resolveInfo?.serviceInfo ?: return@any false
                ComponentName(service.packageName, service.name) == self
            }
    }

    /**
     * True when NexaTV is effectively Home: either the real default Home (resolver) or the HOME-override
     * service is enabled. Either way the HOME button opens NexaTV, so the app shows its grid rather than
     * the pre-Home prompt (ADR-0007).
     */
    fun isEffectivelyHome(): Boolean = isDefaultHome() || isHomeOverrideEnabled()

    /**
     * Intent for the prompt's call-to-action. On this TCL `ROLE_HOME` replacement is non-functional
     * (ADR-0006 validation), so the working, reversible path is the HOME-override accessibility service:
     * send the owner to Settings → Accessibility to enable it (ADR-0007). Where the role is genuinely
     * available we still prefer the first-party role request.
     */
    fun requestHomeIntent(): Intent =
        roleManager
            ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_HOME) }
            ?.createRequestRoleIntent(RoleManager.ROLE_HOME)
            ?: Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
