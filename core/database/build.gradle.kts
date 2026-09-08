plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.focus.database"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.compiler)
    implementation(libs.kotlinx.coroutines.core)
}
