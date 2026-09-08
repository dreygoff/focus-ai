plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
}
