plugins {
    alias(libs.plugins.cineverse.android.library)
    alias(libs.plugins.cineverse.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cineverse.core.network"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"https://api.cineverse.example/\"")
    }
    buildTypes {
        // Debug builds answer catalog calls from MockCatalogInterceptor until a real backend exists.
        getByName("debug") {
            buildConfigField("boolean", "USE_MOCK_API", "true")
        }
        getByName("release") {
            buildConfigField("boolean", "USE_MOCK_API", "false")
        }
    }
}

dependencies {
    implementation(projects.core.common)
    api(libs.retrofit.core)
    api(libs.okhttp.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
}
