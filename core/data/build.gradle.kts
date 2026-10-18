plugins {
    alias(libs.plugins.cineverse.android.library)
    alias(libs.plugins.cineverse.hilt)
}

android {
    namespace = "com.cineverse.core.data"
}

dependencies {
    // Implements the interfaces declared in :core:domain using network + datastore.
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)
    implementation(projects.core.datastore)
}
