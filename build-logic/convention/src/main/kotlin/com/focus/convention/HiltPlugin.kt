package com.focus.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Hilt dependency injection.
 */
class HiltPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.dagger.hilt.android")

            val hiltVersion = libs.findVersion("hilt").get()

            dependencies {
                add("implementation", "com.google.dagger:hilt-android:$hiltVersion")
                add("ksp", "com.google.dagger:hilt-android-compiler:$hiltVersion")

                // Add Hilt testing dependencies for test modules
                add("testImplementation", "com.google.dagger:hilt-android:$hiltVersion")
            }
        }
    }
}
