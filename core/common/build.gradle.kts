plugins {
    alias(libs.plugins.cineverse.android.library)
    alias(libs.plugins.cineverse.hilt)
}

android {
    namespace = "com.cineverse.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
}
