plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.focus.notifications"
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
    api(project(":core:common"))
    implementation(libs.com.jakewharton.timber)
    implementation(libs.androidx.core.ktx)
}
