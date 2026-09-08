package com.focus.convention.android

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Convention plugin for Android Application modules.
 * Applies AGP 9.x, KSP, Compose compiler config (Kotlin is built into AGP 9+).
 */
class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("com.google.devtools.ksp")

            extensions.configure(ApplicationExtension::class.java) { ext ->
                ext.compileSdk = 37

                ext.defaultConfig {
                    applicationId = "app.focus.android"
                    minSdk = 26
                    targetSdk = 36
                    versionCode = 10000
                    versionName = "1.0.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
                }

                ext.buildTypes {
                    getByName("debug") {
                        isDebuggable = true
                        isMinifyEnabled = false
                    }
                    getByName("release") {
                        isMinifyEnabled = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                ext.compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                ext.lint {
                    lintConfig = rootProject.file("lint.xml")
                }
            }
            extensions.configure(KotlinAndroidProjectExtension::class.java) { ext ->
                ext.compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
}
