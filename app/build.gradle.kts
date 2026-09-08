plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.focus.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.focus.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 10000
        versionName = "1.0.0"
        
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.org.jetbrains.kotlinx.json)

    implementation(libs.com.jakewharton.timber)

    api(project(":core:domain"))
    api(project(":core:data"))
    api(project(":core:database"))
    api(project(":core:datastore"))
    api(project(":core:common"))
    api(project(":core:system"))
    api(project(":core:designsystem"))
    api(project(":core:ui"))
    api(project(":core:notifications"))

    api(project(":feature:onboarding"))
    api(project(":feature:home"))
    api(project(":feature:profiles"))
    api(project(":feature:session"))
    api(project(":feature:blocker"))
    api(project(":feature:schedules"))
    api(project(":feature:stats"))
    api(project(":feature:settings"))

    implementation(project(":service:focus-service"))
    implementation(project(":service:accessibility"))

    testImplementation(libs.junit)
}
