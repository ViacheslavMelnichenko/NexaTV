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

// Release versioning (ADR-0003 D5). CI passes -PversionName/-PversionCode derived from the git tag;
// local/dev and CI non-release builds fall back to the in-file defaults so they always build.
val releaseVersionName = (project.findProperty("versionName") as String?) ?: "0.2.1"
val releaseVersionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 201

android {
    namespace = "com.nexatv.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nexatv.launcher"
        // Single device target: TCL Android TV OS 14 (API 34). See ADR-0002.
        minSdk = 34
        targetSdk = 34
        // NFR-011: versionCode = MAJOR*10000 + MINOR*100 + PATCH (0.1.0 -> 100). Overridable from the
        // git tag via -PversionCode/-PversionName in release CI; defaults above keep local builds working.
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        buildConfigField("boolean", "IS_PREVIEW", "false")
        vectorDrawables.useSupportLibrary = true

        // Single device target is 32-bit armeabi-v7a (ADR-0002), so keep only that ABI's native libs
        // (e.g. DataStore's libdatastore_shared_counter.so, which otherwise ships for all four ABIs).
        // Keeps the APK lean (NFR-002) and satisfies the CI "armeabi-v7a only" ABI gate — a device
        // that can't run 64-bit/x86 code should never carry those .so files.
        ndk { abiFilters += "armeabi-v7a" }
    }

    // Release signing (ADR-0003 D3/D4). The key is read from environment variables injected by release
    // CI from encrypted secrets — never from Git. When absent (local/dev), the release build is left
    // UNSIGNED rather than failing, and CI fails fast earlier if a *release* is attempted without secrets.
    val keystorePath = System.getenv("NEXATV_KEYSTORE_FILE")
    val hasKeystore = keystorePath != null && file(keystorePath).exists()
    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("NEXATV_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NEXATV_KEY_ALIAS")
                keyPassword = System.getenv("NEXATV_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            buildConfigField("boolean", "IS_PREVIEW", "true")
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signed only when the keystore env is present (release CI); unsigned locally (ADR-0003 D3/D4).
            signingConfig = if (hasKeystore) signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        // Expose versionName to the UI (Settings shows a faint build version). BuildConfig is off by
        // default in AGP 8; enable only this one field, nothing else depends on it.
        buildConfig = true
    }
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
    // Real org.json so the WeatherRepository parser (ADR-0010) is testable on the JVM; android.jar
    // ships only stubs. Test-only — production uses the on-device org.json, APK unaffected (NFR-002).
    testImplementation(libs.json)
}
