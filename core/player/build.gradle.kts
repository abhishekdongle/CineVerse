// Player library: Media3 ExoPlayer, Cast, IMA ads and MediaSession behind a UI-free API
// (PlayerController + StateFlow<PlayerState>). No screens, composables or ViewModels, and no
// dependencies on other project modules, so any feature (phone or TV UI) can reuse it.
plugins {
    alias(libs.plugins.cineverse.android.library)
    alias(libs.plugins.cineverse.hilt)
}

android {
    namespace = "com.cineverse.core.player"
}

dependencies {
    api(libs.androidx.media3.exoplayer)
    api(libs.androidx.media3.session)
    api(libs.androidx.media3.cast)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.ima)
    implementation(libs.play.services.base)
    implementation(libs.kotlinx.coroutines.android)
}
