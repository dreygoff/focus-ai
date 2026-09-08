package com.focus.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Compose configuration.
 * Applies Compose compiler and adds BOM dependencies.
 */
class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            dependencies {
                val bom = libs.platform("androidx-compose-bom")
                add("implementation", platform(bom))

                // Debug tooling
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())

                // Core Compose libraries from BOM
                add("implementation", libs.findLibrary("androidx-compose-ui").get())
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-compose-foundation").get())
                add("implementation", libs.findLibrary("androidx-compose-runtime").get())
                add("implementation", libs.findLibrary("androidx-compose-material-icons-extended").get())
            }
        }
    }
}
