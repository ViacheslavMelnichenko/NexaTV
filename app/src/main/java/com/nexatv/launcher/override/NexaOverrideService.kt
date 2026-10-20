/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.override

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.nexatv.launcher.MainActivity

/**
 * Prototype HOME override (ADR-0006 follow-up). On this TCL Google TV firmware a third-party launcher
 * cannot become the effective HOME via `ROLE_HOME` — the HOME button always opens stock Google TV
 * (`docs/technical/ANDROID-TV.md` §3b). The documented, reversible, stock-launcher-safe workaround is
 * the same one Projectivy uses: an accessibility service that watches for the stock launcher coming to
 * the foreground (which is what HOME triggers) and immediately raises NexaTV over it.
 *
 * This does **not** intercept the HOME key (the system consumes it first) and does **not** disable the
 * stock launcher; it is fully reversible by turning the service off in Settings → Accessibility.
 *
 * Caveats (to validate on-device): a 2025 Google TV update reportedly broke this override on some
 * firmware, and the Back button may still return to stock Google TV. Prototype scope only.
 */
class NexaOverrideService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != STOCK_LAUNCHER_PACKAGE) return

        // Stock Google TV just came to the front (HOME press / wake) — raise NexaTV over it.
        val home = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(home)
    }

    override fun onInterrupt() = Unit

    private companion object {
        /** Stock Google TV launcher on this TCL (see ANDROID-TV.md §2). */
        const val STOCK_LAUNCHER_PACKAGE = "com.google.android.apps.tv.launcherx"
    }
}
