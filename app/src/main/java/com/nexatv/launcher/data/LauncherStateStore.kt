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
import com.nexatv.launcher.data.proto.LauncherStateProto
import com.nexatv.launcher.data.proto.LauncherStateSerializer

/**
 * The single app-internal launcher-state store (ADR-0008). Declared once and shared by every
 * repository (favorites, settings, …) because DataStore allows **one** active store per file — a
 * second `dataStore(fileName = "launcher_state.pb")` delegate would crash at runtime. Local only,
 * consistent with no-INTERNET / `allowBackup=false` (NFR-021).
 */
internal val Context.launcherStateStore: DataStore<LauncherStateProto> by dataStore(
    fileName = "launcher_state.pb",
    serializer = LauncherStateSerializer,
)
