plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.focus.feature.widget"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}
