pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "focus"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

// Core modules
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:system")
include(":core:designsystem")
include(":core:ui")
include(":core:notifications")
include(":core:testing")

// Feature modules
include(":feature:onboarding")
include(":feature:permissions")
include(":feature:home")
include(":feature:profiles")
include(":feature:session")
include(":feature:blocker")
include(":feature:schedules")
include(":feature:stats")
include(":feature:settings")
include(":feature:widget")

// Service modules
include(":service:focus-service")
include(":service:accessibility")

// Centralize module build outputs under root build/ to reduce Windows lint-cache file locks
// when IDE/antivirus hold handles under per-module build/ directories.
gradle.beforeProject {
    if (this != rootProject) {
        val outputPath = path.removePrefix(":").replace(':', '/')
        layout.buildDirectory.set(rootProject.layout.buildDirectory.dir(outputPath))
    }
}

// Avoid JVM jar URL connection caches holding lint migrated-jar handles on Windows.
java.net.URL("jar:file:///dummy.jar!/").openConnection().setDefaultUseCaches(false)
