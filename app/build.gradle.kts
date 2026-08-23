plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.python.localhost"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.python.localhost"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Chaquopy also supports x86, but arm64 + armv7 covers virtually all phones.
            // Add "x86" / "x86_64" here if you need emulator support on those ABIs.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        python {
            // Embedded CPython version for v1. The Runtime Manager architecture allows
            // additional versions to be added later (see core/runtime).
            version = "3.11"
            pip {
                // Runtime dependency installation happens from inside the app via the
                // Dependency Manager (pip is driven from the embedded interpreter).
                // Nothing is baked into the APK by default, keeping the base APK small.
            }
        }
    }

    signingConfigs {
        create("release") {
            // Release signing is sourced from environment variables so keystores/passwords
            // are NEVER committed to the repository.
            val ks = System.getenv("PYMOBILE_KEYSTORE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("PYMOBILE_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("PYMOBILE_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("PYMOBILE_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signing is configured from environment variables (never committed).
            // Set PYMOBILE_KEYSTORE (plus the matching password/alias vars) in CI/local env.
            if (System.getenv("PYMOBILE_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // For Kotlin 1.9.x the Compose compiler is configured via composeOptions.
    // The org.jetbrains.kotlin.plugin.compose Gradle plugin is only available for Kotlin 2.0+.
    // Compose compiler 1.5.14 is the version compatible with Kotlin 1.9.24.
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LGPL3}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/NOTICE"
            excludes += "**/*.kotlin_module"
            excludes += "/META-INF/INDEX.LIST"
        }
        jniLibs {
            // Chaquopy ships the Python shared libraries; keep defaults.
        }
    }
}

dependencies {
    val composeBom = "androidx.compose:compose-bom:2024.09.00"
    implementation(platform(composeBom))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.security:security-crypto:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
    implementation("org.slf4j:slf4j-nop:2.0.16")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito:mockito-core:5.11.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
