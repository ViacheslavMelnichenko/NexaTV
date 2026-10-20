// NexaTV — Gradle settings. Licensed under the Apache License, Version 2.0.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NexaTV"
include(":app")
// Device-gated performance harness: Macrobenchmark + Baseline Profile generator (measures :app).
include(":benchmark")
