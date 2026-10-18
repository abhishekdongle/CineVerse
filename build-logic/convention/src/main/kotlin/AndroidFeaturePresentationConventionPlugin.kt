import com.cineverse.buildlogic.libs
import com.cineverse.buildlogic.library
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * :feature:<name>:presentation — ViewModels and UI state. No Compose, no platform UI, so the
 * same ViewModel serves the mobile and the TV screens and is tested with plain unit tests.
 * Applying the Compose compiler or adding a Compose/Material dependency fails the build.
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
            pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
                throw GradleException("$path is a presentation module and must not use Compose; put composables in the :ui module.")
            }
            configurations.configureEach {
                val configurationName = name
                dependencies.withType<ExternalModuleDependency>().configureEach {
                    if (group?.startsWith("androidx.compose") == true) {
                        throw GradleException(
                            "$path is a presentation module and must not depend on $group:$name " +
                                "(configuration '$configurationName'); put Compose code in the :ui module.",
                        )
                    }
                }
            }
        }
    }
}
