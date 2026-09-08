package com.focus.convention

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Convention plugin for Android Library modules (AGP 9.x, built-in Kotlin).
 */
class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("com.google.devtools.ksp")

            extensions.configure(LibraryExtension::class.java) { ext ->
                ext.compileSdk = 37

                ext.defaultConfig {
                    minSdk = 26
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                ext.buildTypes {
                    getByName("debug") { isDebuggable = true }
                    getByName("release") {
                        isMinifyEnabled = false
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
