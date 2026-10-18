pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "CineVerse"

// Apps: one installable APK per platform
include(":app:mobile")
include(":app:tv")

// Core
include(":core:common")
include(":core:network")
include(":core:data")
include(":core:domain")
include(":core:datastore")
include(":core:player")
include(":core:analytics")
include(":core:notifications")

// Features
include(":feature:home:presentation")
include(":feature:home:ui")
include(":feature:videoplayer:presentation")
include(":feature:videoplayer:ui")
