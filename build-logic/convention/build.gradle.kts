plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

dependencies {
    implementation("com.android.tools.build:gradle:8.5.2")
    implementation(kotlin("gradle-plugin", "2.0.20"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.20")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.20-1.0.25")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.7")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
}
