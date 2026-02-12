plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt") // ضروري لتشغيل kapt مع Room أو أي مكتبة تستخدم annotation processing
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pronetwork.app" // غيرها إذا عندك اسم مختلف في مشروعك
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pronetwork.app" // غيرها إذا عندك اسم مختلف في مشروعك
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // حل مشكلة اختلاف إصدار الجافا بين kapt و javac
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

    // Compose BOM (لتوحيد النسخ)
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI & Foundation
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")

    // Compose Material 2
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Material 3
    implementation("androidx.compose.material3:material3")

    // Compose Tooling & Preview
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.8.2")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.compose.runtime:runtime-livedata")

    // rememberSaveable
    implementation("androidx.compose.runtime:runtime-saveable")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
}

// يمكنك حذف هذا إذا لا تستخدم Dagger Hilt أو kapt في مكتبات أخرى
kapt {
    correctErrorTypes = true
}