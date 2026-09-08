package com.focus.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for app module — applies AndroidApplication + Hilt + Room plugins via aliases.
 */
class AppPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // The base application config is already applied by AndroidApplicationPlugin.
            // This plugin just exists to satisfy the settings.gradle.kts include convention naming.
        }
    }
}
