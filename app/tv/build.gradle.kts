plugins {
    alias(libs.plugins.cineverse.android.application.compose)
    alias(libs.plugins.cineverse.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cineverse.tv"

    defaultConfig {
        // Separate id so the TV build can be installed next to the mobile build on one device.
        // Use "com.cineverse" for both if they ship as a single Play listing.
        applicationId = "com.cineverse.tv"
        minSdk = libs.versions.minSdkTv.get().toInt()
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
    implementation(projects.core.player)
    implementation(projects.core.notifications)
    // Not used from app code: it puts the repository bindings into this app's Hilt graph.
    implementation(projects.core.data)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
}
