package com.cineverse.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** SDK levels and Java target shared by every Android module (apps and libraries). */
internal fun Project.configureAndroid(extension: CommonExtension) {
    extension.compileSdk = libs.intVersion("compileSdk")
    extension.defaultConfig.minSdk = libs.intVersion("minSdk")
    extension.defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // AGP 9 compiles Kotlin itself and derives the Kotlin jvmTarget from these.
    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_17
}

/** Turns on Compose and adds the BOM plus the artifacts every Compose module needs. */
internal fun Project.configureCompose(extension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true

    dependencies {
        val bom = platform(libs.library("androidx-compose-bom"))
        add("implementation", bom)
        add("androidTestImplementation", bom)
        add("implementation", libs.library("androidx-compose-ui"))
        add("implementation", libs.library("androidx-compose-ui-graphics"))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
    }
}
