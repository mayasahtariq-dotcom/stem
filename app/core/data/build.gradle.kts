plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.stemtutor.core.data"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:network"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Declared now per the tech stack; used from M1 (auth) and later milestones (caching).
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // M3 Listen: plays lesson audio and keeps up to 50 MB of it on the phone.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource)
    implementation(libs.media3.database)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
