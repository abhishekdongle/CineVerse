plugins {
    alias(libs.plugins.cineverse.android.library)
    alias(libs.plugins.cineverse.hilt)
}

android {
    namespace = "com.cineverse.core.datastore"
}

dependencies {
    implementation(projects.core.common)
    api(libs.androidx.datastore.preferences)
}
