plugins {
     // ... existing plugins ...
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.neotune"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.neotune"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
    buildToolsVersion = "36.0.0"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.junit.junit)
    implementation(libs.androidx.constraintlayout.compose.android)
    implementation(libs.androidx.compose.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Ktor HTTP client (CIO engine)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // ExoPlayer for audio playback
    implementation(libs.exoplayer)

    // Kotlinx Serialization (for JSON parsing)
    implementation(libs.kotlinx.serialization.json)

    // Coil for image loading
    implementation(libs.coil)

    // Coroutines (for async work)
    implementation(libs.kotlinx.coroutines.android)
    implementation(kotlin("test"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    // Compose ViewModel support
    implementation(libs.androidx.lifecycle.viewmodel.compose.v270)
    // Coil Compose for image loading
    implementation(libs.coil.compose.v240)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.amdroidx.media3.session)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.palette.ktx)
}