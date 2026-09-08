// Root build.gradle.kts - Focus Android Project
// Digital wellbeing application with modular architecture
//
// NOTE: Common dependencies (e.g., Compose BOM) are managed via convention plugins
// in build-logic/ as specified in the project architecture.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.protobuf) apply false
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

subprojects {
    plugins.withId("com.android.base") {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            baseline = file("${layout.projectDirectory.asFile}/detekt-baseline.xml")
        }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            baseline = file("${layout.projectDirectory.asFile}/detekt-baseline.xml")
        }
    }
}
