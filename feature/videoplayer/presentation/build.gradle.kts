// Video player screen logic: VideoPlayerViewModel, VideoPlayerUiState and VideoPlayerAction.
// No Compose or Material dependencies (the convention plugin rejects them), so the ViewModel is
// tested with plain JVM unit tests and can drive both the phone and the TV screen.
plugins {
    alias(libs.plugins.cineverse.android.feature.presentation)
}

android {
    namespace = "com.cineverse.feature.videoplayer.presentation"
}

dependencies {
    // api: the ViewModel's public API takes PlaybackRequest and exposes the Media3 Player.
    api(projects.core.player)
}
