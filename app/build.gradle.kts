import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun config(name: String, default: String): String =
    localProps.getProperty(name) ?: providers.environmentVariable(name).orNull ?: default

val supabaseUrl = config("SUPABASE_URL", "http://10.0.2.2:54321")
val supabaseAnonKey = config("SUPABASE_ANON_KEY", "")
// Must match app_config.allowed_email_domain; only used to word the sign-up error.
val allowedEmailDomain = config("ALLOWED_EMAIL_DOMAIN", "student.example.ac.id")
// Without a key there is no backend to talk to, so fall back to the in-memory demo data.
val useFakeBackend = config("USE_FAKE_BACKEND", (supabaseAnonKey.isBlank()).toString()).toBoolean()

android {
    namespace = "dev.ether.app"
    // Current AndroidX and Coil releases refuse to build against anything older; targetSdk stays 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.ether.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "ALLOWED_EMAIL_DOMAIN", "\"$allowedEmailDomain\"")
        buildConfigField("boolean", "USE_FAKE_BACKEND", useFakeBackend.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
