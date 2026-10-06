import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "ai.flux.mobile"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    val fluxSigningStore = System.getenv("FLUX_SIGNING_STORE_FILE")
    val hasFluxSigning = !fluxSigningStore.isNullOrBlank()
    val localBuildProperties = Properties().apply {
        val propertiesFile = rootProject.file("local.properties")
        if (propertiesFile.exists()) propertiesFile.inputStream().use(::load)
    }
    val fluxCoreUrl = System.getenv("FLUX_CORE_URL").orEmpty()
        .ifBlank { localBuildProperties.getProperty("flux.core.url").orEmpty() }
    fun buildConfigString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    defaultConfig {
        applicationId = "ai.flux.mobile"
        minSdk = 28
        targetSdk = 35
        versionCode = 37
        versionName = "1.8.0"

        buildConfigField("String", "FLUX_CORE_URL", buildConfigString(fluxCoreUrl))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (hasFluxSigning) {
            create("fluxRelease") {
                storeFile = file(fluxSigningStore!!)
                storePassword = System.getenv("FLUX_SIGNING_STORE_PASSWORD")
                    ?: error("FLUX_SIGNING_STORE_PASSWORD is required")
                keyAlias = System.getenv("FLUX_SIGNING_KEY_ALIAS")
                    ?: error("FLUX_SIGNING_KEY_ALIAS is required")
                keyPassword = System.getenv("FLUX_SIGNING_KEY_PASSWORD")
                    ?: error("FLUX_SIGNING_KEY_PASSWORD is required")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasFluxSigning) {
                signingConfigs.getByName("fluxRelease")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AndroidX lifecycle 2.8.x ships a lint detector that is incompatible
    // with the Kotlin analysis API used by this Android Gradle Plugin.
    // Disable only the crashing detector; all other lint checks stay enabled.
    lint {
        disable += "NullSafeMutableLiveData"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
