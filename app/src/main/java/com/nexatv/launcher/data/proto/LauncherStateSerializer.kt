/*
 * Copyright 2026 Viacheslav Melnichenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.nexatv.launcher.data.proto

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/**
 * Proto DataStore serializer for [LauncherStateProto] (ADR-0008). The default value is the
 * empty proto, so a first launch (or a reset store) reads as "no favorites" rather than failing.
 * A parse failure surfaces as [CorruptionException], which DataStore handles per its corruption
 * policy instead of crashing the app.
 */
object LauncherStateSerializer : Serializer<LauncherStateProto> {

    override val defaultValue: LauncherStateProto = LauncherStateProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LauncherStateProto =
        try {
            LauncherStateProto.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read launcher state proto.", e)
        }

    override suspend fun writeTo(t: LauncherStateProto, output: OutputStream) = t.writeTo(output)
}
