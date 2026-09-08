plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines.core)
    api(project(":core:common"))
    testImplementation(libs.junit)
}
