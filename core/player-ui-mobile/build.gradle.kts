// Compose player UI for phones, tablets and foldables, built only on :core:player's
// PlayerController API. A TV module (:core:player-ui-tv) would sit next to this one.
plugins {
    alias(libs.plugins.cineverse.android.library.compose)
}

android {
    namespace = "com.cineverse.core.player.ui.mobile"
}

dependencies {
    api(projects.core.player)
    implementation(libs.androidx.media3.ui.compose)
    // SubtitleView only: renders cues with the system caption style (CaptioningManager).
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.cast)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
}
