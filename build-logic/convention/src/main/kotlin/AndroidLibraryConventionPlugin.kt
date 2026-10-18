import com.android.build.api.dsl.LibraryExtension
import com.cineverse.buildlogic.configureAndroid
import com.cineverse.buildlogic.enforceModuleGraphRules
import com.cineverse.buildlogic.libs
import com.cineverse.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }
            dependencies {
                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
            enforceModuleGraphRules()
        }
    }
}
