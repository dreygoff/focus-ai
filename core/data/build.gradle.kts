plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.focus.data"
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
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:common"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.compiler)
    implementation(libs.kotlinx.coroutines.core)
}
