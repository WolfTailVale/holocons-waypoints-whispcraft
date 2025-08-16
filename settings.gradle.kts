pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    // Automatically resolve and download required JDK toolchains
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "WhispWaypoints"
