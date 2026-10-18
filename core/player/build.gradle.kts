// Player engine: Media3 ExoPlayer, Cast, IMA ads and MediaSession behind a UI-free API
// (PlayerController + StateFlow<PlayerState>). Phone UI lives in :core:player-ui-mobile; a TV UI
// module can use this module unchanged. No dependencies on other project modules.
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
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
