plugins {
    alias(libs.plugins.cineverse.android.library)
    alias(libs.plugins.cineverse.hilt)
}

android {
    namespace = "com.cineverse.core.notifications"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.analytics)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
