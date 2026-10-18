plugins {
    alias(libs.plugins.cineverse.android.application.compose)
    alias(libs.plugins.cineverse.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cineverse.mobile"

    defaultConfig {
        applicationId = "com.cineverse"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
}

dependencies {
    implementation(projects.feature.home.ui)
    implementation(projects.core.domain)
    implementation(projects.feature.videoplayer.ui)
    // DI setup only: Cast initialization and the PlaybackAnalyticsListener binding.
    implementation(projects.core.player)
    implementation(projects.core.analytics)
    implementation(projects.core.notifications)
    // Not used from app code: it puts the repository bindings into this app's Hilt graph.
    implementation(projects.core.data)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
