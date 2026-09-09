plugins {
    id("com.android.library")
}

android {
    namespace = "app.focus.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.compose.runtime:runtime:1.7.8")
    implementation(project(":core:domain"))
}
