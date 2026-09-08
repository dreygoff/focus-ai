package com.focus.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Room database configuration.
 */
class RoomPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            if (pluginManager.hasPlugin("com.android.application") ||
                pluginManager.hasPlugin("com.android.library")) {
                pluginManager.apply("androidx.room")
            } else {
                // For JVM modules, add Room KSP compiler
                pluginManager.apply("com.google.devtools.ksp")
            }

            dependencies {
                val roomVersion = libs.findVersion("room").get()

                // Add Room dependencies if we're in an Android module
                if (pluginManager.hasPlugin("com.android.application") ||
                    pluginManager.hasPlugin("com.android.library")) {
                    add("implementation", "androidx.room:room-runtime:$roomVersion")
                    add("implementation", "androidx.room:room-ktx:$roomVersion")
                    add("ksp", "androidx.room:room-compiler:$roomVersion")
                } else {
                    // JVM-only module (like core:data) needs just KSP for compiler
                    add("implementation", "androidx.room:room-runtime:$roomVersion")
                    add("implementation", "androidx.room:room-ktx:$roomVersion")
                    add("ksp", "androidx.room:room-compiler:$roomVersion")
                }
            }
        }
    }
}
