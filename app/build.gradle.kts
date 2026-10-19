plugins {
    alias(libs.plugins.android.application)
    // Kotlin itself is built into AGP 9; only the Compose compiler plugin is applied.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.kotlincalculator"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.kotlincalculator"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // With built-in Kotlin, the Kotlin jvmTarget follows targetCompatibility,
    // so no kotlinOptions / kotlin.compilerOptions block is needed.

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}

// Native Notify: Firebase starts once google-services.json is in this folder, so the project still builds before you add it.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
