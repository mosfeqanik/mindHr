plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.mindhr.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mindhr.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    // API base URL per build type; -PapiBaseUrl=... overrides both (e.g. http://192.168.1.10:8080/ for a real phone).
    val apiBaseUrlOverride = project.findProperty("apiBaseUrl") as String?
    buildTypes {
        debug {
            // 10.0.2.2 = host machine as seen from the Android emulator (local backend).
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrlOverride ?: "http://10.0.2.2:8080/"}\"")
        }
        release {
            isMinifyEnabled = false
            // Live backend on Render.
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrlOverride ?: "https://mindhr.onrender.com/"}\"")
            // Signed with the debug key so the APK installs for testing. Use a real keystore before the Play Store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
}
