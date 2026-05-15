plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.paryavarankavalu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.paryavarankavalu"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Material Components (XML)
    implementation("com.google.android.material:material:1.12.0")

    // Maps + Location
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")

    // AI
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    // Image
    implementation("io.coil-kt:coil-compose:2.6.0")

    // EXIF
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}