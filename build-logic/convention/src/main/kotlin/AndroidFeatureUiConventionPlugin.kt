import com.cineverse.buildlogic.libs
import com.cineverse.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** :feature:<name>:ui — Compose screens for mobile (Material 3) and TV (TV Material). */
class AndroidFeatureUiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("cineverse.android.library.compose")
            dependencies {
                add("implementation", project(":core:common"))
                add("implementation", libs.library("androidx-compose-foundation"))
                add("implementation", libs.library("androidx-compose-material3"))
                add("implementation", libs.library("androidx-tv-material"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            }
        }
    }
}
