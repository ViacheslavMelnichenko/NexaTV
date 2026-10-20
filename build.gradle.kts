// NexaTV — root build script. Licensed under the Apache License, Version 2.0.
// Plugins are declared here (apply false) and applied in module build scripts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.detekt) apply false
}
