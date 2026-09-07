package com.focus.convention.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Convention plugin for Android Application modules.
 * Applies AGP, Kotlin, KSP, Hilt, Compose config.
 */
class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply base plugins
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("com.google.devtools.ksp")

            // Configure AGP
            val ext = extensions.getByType(BaseExtension::class.java)
            with(ext) {
                compileSdk = 35

                defaultConfig {
                    applicationId = "app.focus.android"
                    minSdk = 26
                    targetSdk = 35
                    versionCode = 10000
                    versionName = "1.0.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                    // Room schema location
                    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
                }

                buildTypes {
                    debug {
                        isDebuggable = true
                        isMinifyEnabled = false
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    }
                    release {
                        isMinifyEnabled = true
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                kotlinOptions { jvmTarget = "17" }
                buildFeatures { compose = true; viewBinding = false; xml = false }
                // Compose compiler config is handled by compose.compiler plugin
            }
        }
    }
}
