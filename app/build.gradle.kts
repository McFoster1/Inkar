plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appChannel = (project.findProperty("appChannel") as String?) ?: "dev"
val isStable = appChannel == "stable"

android {
    namespace = "com.mcfoster.inkar"
    compileSdk = 35

    defaultConfig {
        // stable = the real app, dev = test app that installs next to it.
        applicationId = if (isStable) "com.mcfoster.inkar" else "com.mcfoster.inkar.dev"
        manifestPlaceholders["appLabel"] = if (isStable) "Inkar" else "Inkar Dev"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
}
