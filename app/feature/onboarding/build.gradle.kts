plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.stemtutor.feature.onboarding"
    buildFeatures.compose = true
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
}
