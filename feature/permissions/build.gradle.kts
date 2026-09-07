plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.focus.feature.permissions"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.8")
    
    api("com.google.dagger:hilt-android:2.55.0")
    ksp("com.google.dagger:hilt-compiler:2.55.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:system"))
    implementation(project(":core:common"))
}
