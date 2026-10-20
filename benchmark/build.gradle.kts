// NexaTV — :benchmark module. Licensed under the Apache License, Version 2.0.
//
// A com.android.test module that MEASURES :app on a real device: Macrobenchmark (startup +
// frame timing) and the Baseline Profile generator. It ships no app code and is never part of a
// release build — it exists only to produce numbers and the profile. Every task here is
// device-gated: it needs a connected TCL (see README.md). The baselineprofile plugin wires this
// module's output into :app and manages the matching build variants on both sides.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.nexatv.benchmark"
    compileSdk = 34

    defaultConfig {
        // Matches the single device target (TCL Android TV OS 14, API 34 — ADR-0002).
        minSdk = 34
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // This test APK instruments the launcher; self-instrumenting keeps both in one process group.
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

// Generate the profile from a connected device (no managed/Gradle-Managed-Device emulator for TV).
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
