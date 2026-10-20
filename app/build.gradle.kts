// NexaTV — :app module. Licensed under the Apache License, Version 2.0.
import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.detekt)
    // Proto DataStore codegen (ADR-0008): compiles src/main/proto into Java-lite classes.
    alias(libs.plugins.protobuf)
    // Baseline Profile consumer side: wires the generated profile into the release APK and
    // auto-creates the nonMinifiedRelease / benchmarkRelease build types the :benchmark module
    // measures. Generating the actual profile is device-gated (see :benchmark/README.md).
    alias(libs.plugins.baselineprofile)
}

// Static analysis gate (ADR-0003 D1 / NFR-008). Config: config/detekt/detekt.yml.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    autoCorrect = false
}

android {
    namespace = "com.nexatv.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nexatv.launcher"
        // Single device target: TCL Android TV OS 14 (API 34). See ADR-0002 / initial.md §19.
        minSdk = 34
        targetSdk = 34
        // NFR-011: versionCode = MAJOR*10000 + MINOR*100 + PATCH. 0.1.0 -> 100.
        versionCode = 100
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signing is wired in CI from secrets (ADR-0003 / NFR-010). No keys in Git.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Protobuf-lite codegen for Proto DataStore (ADR-0008). Lite runtime keeps the APK small
// (NFR-002) and is the variant DataStore expects on Android.
protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") { option("lite") }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Compose-for-TV material (androidx.tv) — D-pad-first components (ADR-0002).
    implementation(libs.androidx.tv.material)

    // Local persistence — Proto DataStore + Protobuf-lite runtime (ADR-0008).
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.javalite)

    // Installs the generated Baseline Profile at first run so hot code is AOT-compiled (NFR-001).
    implementation(libs.androidx.profileinstaller)
    // Producer of that profile + the macrobenchmarks (device-gated run; see :benchmark).
    baselineProfile(project(":benchmark"))

    // Unit tests (JVM, run in CI — NFR-008). No device/Robolectric needed.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
