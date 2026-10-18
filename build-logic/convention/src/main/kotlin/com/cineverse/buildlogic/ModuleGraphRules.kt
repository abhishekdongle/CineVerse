package com.cineverse.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.withType

/**
 * Architecture rules for project-to-project dependencies. Every convention plugin applies
 * [enforceModuleGraphRules], so adding a forbidden `project(...)` dependency fails the build
 * at configuration time with an explanation.
 */
object ModuleGraphRules {

    /** Core modules that hold implementations. Features reach them only through :core:domain. */
    private val dataLayerModules = setOf(":core:data", ":core:network", ":core:datastore")

    /** Returns why [from] must not depend on [to], or null when the dependency is allowed. */
    fun findViolation(from: String, to: String): String? {
        if (from == to) return null
        val fromFeature = featureName(from)
        val toFeature = featureName(to)
        return when {
            to.startsWith(":app:") ->
                "app modules are entry points; nothing may depend on them."

            from == ":core:domain" ->
                ":core:domain is pure Kotlin and must not depend on other modules."

            from.startsWith(":core:") && toFeature != null ->
                "core modules must not depend on feature modules."

            from.startsWith(":app:") && toFeature != null && to.endsWith(":presentation") ->
                "app modules depend on a feature's :ui module, which brings its presentation module."

            fromFeature != null && toFeature != null && fromFeature != toFeature ->
                "features must not depend on each other (:feature:$fromFeature -> :feature:$toFeature). " +
                    "Move the shared code into a :core module, or let the app module wire the two together."

            fromFeature != null && to in dataLayerModules ->
                "features must use :core:domain use cases and interfaces, not $to directly. " +
                    "The app module provides the implementations."

            fromFeature != null && from.endsWith(":presentation") && to.endsWith(":ui") ->
                "presentation must not depend on ui; the ui module depends on presentation."

            else -> null
        }
    }

    private fun featureName(path: String): String? =
        path.split(':').takeIf { it.size >= 3 && it[1] == "feature" }?.get(2)
}

internal fun Project.enforceModuleGraphRules() {
    val from = path
    configurations.configureEach {
        val configurationName = name
        dependencies.withType<ProjectDependency>().configureEach {
            ModuleGraphRules.findViolation(from, path)?.let { reason ->
                throw GradleException(
                    "Module dependency rule violated: $from -> $path " +
                        "(configuration '$configurationName'): $reason",
                )
            }
        }
    }
    if (from == ":core:domain") {
        pluginManager.withPlugin("com.android.base") {
            throw GradleException(":core:domain must stay pure Kotlin; do not apply an Android plugin.")
        }
    }
}
