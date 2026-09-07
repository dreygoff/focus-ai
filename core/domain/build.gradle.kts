plugins {
    `java-library`
    kotlin
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(kotlin("stdlib"))

    api(libs.kotlinx.coroutines.core)
    api(libs.org.jetbrains.kotlinx.json)

    api(project(":core:common"))

    testImplementation(libs.junit)
}
