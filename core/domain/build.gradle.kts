// Pure Kotlin: no Android plugin, no Android SDK. ModuleGraphRules fails the build if either is added.
plugins {
    alias(libs.plugins.cineverse.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
}
