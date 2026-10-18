plugins {
    alias(libs.plugins.cineverse.android.library.compose)
    alias(libs.plugins.cineverse.hilt)
}

android {
    namespace = "com.cineverse.core.player"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.analytics)
    api(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
}
