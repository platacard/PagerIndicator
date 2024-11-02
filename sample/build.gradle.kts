plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "mx.platacard.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "mx.platacard.pagerindicator.sample"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":pagerindicator"))
    implementation(libs.androidx.appcompat)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
}