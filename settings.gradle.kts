pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "codec"

include(":core-model")
include(":core-media")
include(":core-queue")
include(":core-profiles")
include(":core-settings")
include(":desktop")
include(":android")
