import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.play.PlayPublisherExtension
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI sets these from the GitHub Actions run number so versionCode is
// monotonically increasing across every main-branch build.
val ciVersionCode = (System.getenv("APP_VERSION_CODE") ?: "1").toInt()
val ciVersionName = System.getenv("APP_VERSION_NAME") ?: "1.0.0-dev"

// A machine's own setting: an environment variable, or else a Gradle property. The forge agent's
// environment is fixed when it starts, so on a node these go in ~/.gradle/gradle.properties, which
// every build reads. None of them ever lives in this repository.
fun machineSetting(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

fun git(vararg args: String): String? = runCatching {
    val process = ProcessBuilder("git", *args).directory(rootDir).start()
    process.inputStream.bufferedReader().use { it.readText() }.trim()
        .takeIf { process.waitFor() == 0 && it.isNotEmpty() }
}.getOrNull()

// The Google Play build's versionCode is the commit count of the built ref, so every new commit on the
// branch forge builds from is a higher code. GLYPH_PLAY_VERSION_CODE overrides it. A shallow clone would
// count only its own depth, so publishing refuses one (see the check at the end of this file).
val playVersionCode = machineSetting("GLYPH_PLAY_VERSION_CODE")?.toInt()
    ?: git("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1
val playVersionName = "1.0.$playVersionCode"
val isShallowClone = git("rev-parse", "--is-shallow-repository") == "true"

// The Play upload key lives outside the repository: a properties file (storeFile, storePassword,
// keyAlias, keyPassword) named by GLYPH_UPLOAD_PROPERTIES. It is NOT the committed debug keystore:
// that key and its password are public, so it must never be the key Google Play trusts.
val uploadProperties = machineSetting("GLYPH_UPLOAD_PROPERTIES")?.let { file(it) }?.takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }

android {
    namespace = "com.emberforge.generated.glyphplayground"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ogbar.glyphplayground"
        minSdk = 33
        // Google Play requires apps to target Android 16 (API 36) from
        // Aug 31, 2026. Keep this at the newest stable API level.
        targetSdk = 36
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
        // The Play upload key: only when its properties file is given, never from the repository.
        if (uploadProperties != null) {
            create("upload") {
                storeFile = file(uploadProperties.getProperty("storeFile"))
                storePassword = uploadProperties.getProperty("storePassword")
                keyAlias = uploadProperties.getProperty("keyAlias")
                keyPassword = uploadProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        // The Google Play build. `:app:bundlePlay` makes the AAB, `:app:publishPlayBundle` uploads it
        // (the forge app `glyph-playground-play`). Without the upload key the bundle is unsigned.
        create("play") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            signingConfig = signingConfigs.findByName("upload")
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

// The Play build numbers itself from git (above), not from the GitHub run number the APK channel uses.
androidComponents {
    onVariants(selector().withBuildType("play")) { variant ->
        variant.outputs.forEach {
            it.versionCode.set(playVersionCode)
            it.versionName.set(playVersionName)
        }
    }
}

dependencies {
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
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

// Uploading the play AAB to Google Play (`:app:publishPlayBundle`). The service account's JSON key is
// outside the repository too, named by GLYPH_PLAY_SERVICE_ACCOUNT. Only the play build is ever published.
//
// Applied here, after `androidComponents` above, and not in `plugins {}`: the plugin reads each variant's
// version in its own variant callback, and callbacks run in the order they were registered (the same
// ordering HOIST needs for its Play application id).
apply(plugin = "com.github.triplet.play")
(android as ExtensionAware).extensions.configure<NamedDomainObjectContainer<PlayPublisherExtension>>("playConfigs") {
    register("play") { enabled.set(true) }
}
configure<PlayPublisherExtension> {
    enabled.set(false)
    machineSetting("GLYPH_PLAY_SERVICE_ACCOUNT")?.let { serviceAccountCredentials.set(file(it)) }
    defaultToAppBundles.set(true)
    track.set(machineSetting("GLYPH_PLAY_TRACK") ?: "internal")
    releaseStatus.set(if (machineSetting("GLYPH_PLAY_DRAFT") == "true") ReleaseStatus.DRAFT else ReleaseStatus.COMPLETED)
    // A versionCode Play already has (the same commit built twice) is skipped, not an error.
    resolutionStrategy.set(ResolutionStrategy.IGNORE)
}
// Fail with a plain message instead of Play's: no upload key means an unsigned AAB, and a shallow
// clone means a versionCode that is far too low.
tasks.matching { it.name == "publishPlayBundle" }.configureEach {
    doFirst {
        check(uploadProperties != null) { "GLYPH_UPLOAD_PROPERTIES is not set on this machine: no upload key to sign the play AAB" }
        check(!isShallowClone || machineSetting("GLYPH_PLAY_VERSION_CODE") != null) {
            "shallow clone: the commit count is not the real one; build from a full clone (forge: clone_depth) or set GLYPH_PLAY_VERSION_CODE"
        }
    }
}
