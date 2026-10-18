import com.cineverse.buildlogic.libs
import com.cineverse.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * :feature:<name>:presentation — ViewModels and UI state. No Compose, no platform UI, so the
 * same ViewModel serves the mobile and the TV screens.
 */
class AndroidFeaturePresentationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("cineverse.android.library")
            pluginManager.apply("cineverse.hilt")
            dependencies {
                // api: UI state exposes domain models, so the ui module needs them too.
                add("api", project(":core:domain"))
                add("implementation", project(":core:common"))
                add("implementation", project(":core:analytics"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-ktx"))
                add("implementation", libs.library("kotlinx-coroutines-android"))
            }
        }
    }
}
