plugins {
    id("com.android.library")
}

android {
    namespace = "app.focus.testing"
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
    api(project(":core:domain"))
    implementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
}
