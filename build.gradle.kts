// NexaTV — root build script. Licensed under the Apache License, Version 2.0.
// Plugins are declared here (apply false) and applied in module build scripts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    // AGP 9 has built-in Kotlin: modules no longer apply kotlin-android. Declaring it here pins the
    // Kotlin Gradle Plugin version AGP uses (ADR-0014).
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.baselineprofile) apply false
}
