import com.android.build.api.dsl.LibraryExtension
import com.cineverse.buildlogic.configureCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("cineverse.android.library")
            configureCompose(extensions.getByType<LibraryExtension>())
        }
    }
}
