plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api("javax.inject:javax.inject:1")
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.coroutines.test)
}
