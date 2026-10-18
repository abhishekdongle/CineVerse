// Mobile (Material 3) and TV (TV Material) screens, both driven by HomeViewModel.
plugins {
    alias(libs.plugins.cineverse.android.feature.ui)
}

android {
    namespace = "com.cineverse.feature.home.ui"
}

dependencies {
    api(projects.feature.home.presentation)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
