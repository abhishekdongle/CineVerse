// Compose video player UI for phones, tablets and foldables, plus the interim TV player screen.
// Renders VideoPlayerUiState and sends VideoPlayerAction to VideoPlayerViewModel. :core:player comes
// through the presentation module's api; the UI uses it only for PlaybackRequest and the Media3
// Player drawn by the video surface.
plugins {
    alias(libs.plugins.cineverse.android.feature.ui)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cineverse.feature.videoplayer.ui"
}

dependencies {
    // api: the screen and route take a PlaybackRequest (from :core:player) and overlay slots receive
    // VideoPlayerUiState and VideoPlayerAction.
    api(projects.feature.videoplayer.presentation)
    implementation(libs.androidx.media3.ui.compose)
    // SubtitleView (system caption style) and the TV PlayerView.
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.cast)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
}
