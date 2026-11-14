import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Keys come from local.properties (never committed), falling back to environment variables.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun config(name: String, default: String): String =
    localProperties.getProperty(name) ?: System.getenv(name) ?: default

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.stemtutor.app"

    defaultConfig {
        applicationId = "com.stemtutor.app"
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", quoted(config("API_BASE_URL", "http://10.0.2.2:8000/")))
        buildConfigField("String", "SUPABASE_URL", quoted(config("SUPABASE_URL", "")))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted(config("SUPABASE_ANON_KEY", "")))
        // Web join links: https://<host>/join/<KEY>. A placeholder until a real domain hosts assetlinks.json.
        val joinHost = config("JOIN_LINK_HOST", "mooshala.example")
        buildConfigField("String", "JOIN_LINK_HOST", quoted(joinHost))
        manifestPlaceholders["joinHost"] = joinHost
    }

    buildTypes {
        debug {
            // Fake backends unless USE_REAL_API=true in local.properties.
            buildConfigField("boolean", "USE_REAL_API", config("USE_REAL_API", "false").toBoolean().toString())
        }
        release {
            buildConfigField("boolean", "USE_REAL_API", "true")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:student"))
    implementation(project(":feature:teacher"))
    implementation(project(":feature:parent"))
    implementation(project(":feature:notes"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Declared per the tech stack; first used in M2 (photos) and M3 (audio).
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.coil.compose)
    implementation(libs.media3.exoplayer)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
