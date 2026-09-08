package com.focus.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for feature modules.
 * Applies Android Library + Compose + Hilt plugins.
 */
class AndroidFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.focus.android.library")
            pluginManager.apply("com.focus.compose")
            pluginManager.apply("com.focus.hilt")
        }
    }
}
