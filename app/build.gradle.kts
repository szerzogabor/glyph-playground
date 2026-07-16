plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI sets these from the GitHub Actions run number so versionCode is
// monotonically increasing across every main-branch build.
val ciVersionCode = (System.getenv("APP_VERSION_CODE") ?: "1").toInt()
val ciVersionName = System.getenv("APP_VERSION_NAME") ?: "1.0.0-dev"

android {
    namespace = "com.emberforge.generated.glyphplayground"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.emberforge.generated.glyphplayground"
        minSdk = 24
        targetSdk = 35
        versionCode = ciVersionCode
        versionName = ciVersionName
    }

    signingConfigs {
        // The project's OWN persistent debug keystore, generated and
        // committed by CI on first run (keystore/app-debug.keystore).
        // Android only installs an update over an existing app when the
        // signatures match, so this key must never change.
        create("release") {
            storeFile = rootProject.file("keystore/app-debug.keystore")
            storePassword = "android"
            keyAlias = "appdebug"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
}
