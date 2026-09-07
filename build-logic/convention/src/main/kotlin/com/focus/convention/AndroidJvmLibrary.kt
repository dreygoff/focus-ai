package com.focus.convention

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension

/**
 * Convention plugin for pure JVM (Kotlin) library modules.
 * Used for core:domain, core:data, and other non-Android modules.
 */
class AndroidJvmLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure(JavaPluginExtension::class.java) { ext ->
                ext.sourceCompatibility = JavaVersion.VERSION_17
                ext.targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
