import com.android.build.api.dsl.ApplicationExtension
import com.cineverse.buildlogic.configureAndroid
import com.cineverse.buildlogic.enforceModuleGraphRules
import com.cineverse.buildlogic.intVersion
import com.cineverse.buildlogic.libs
import com.cineverse.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = libs.intVersion("targetSdk")
                // Required by the IMA SDK (via :core:player) and lets libraries use java.time on older APIs.
                compileOptions.isCoreLibraryDesugaringEnabled = true
            }
            dependencies {
                add("coreLibraryDesugaring", libs.library("desugar-jdk-libs"))
            }
            enforceModuleGraphRules()
        }
    }
}
