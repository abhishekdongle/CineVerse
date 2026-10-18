// Shared by :app:mobile and :app:tv. The convention plugin adds Hilt, ViewModel, :core:domain,
// :core:common and :core:analytics.
plugins {
    alias(libs.plugins.cineverse.android.feature.presentation)
}

android {
    namespace = "com.cineverse.feature.home.presentation"
}
