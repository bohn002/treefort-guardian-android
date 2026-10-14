import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.treefort.guardian"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.treefort.guardian"
        minSdk = 24
        targetSdk = 36
        versionCode = 58
        versionName = "0.1.0"
    }

    // Backend and Supabase endpoints come from the environment, `-P` gradle properties or
    // local.properties (never committed). Debug builds fall back to the dev project so a fresh
    // checkout still runs; release builds have no fallback and the app refuses to sign in
    // without them (see AppConfig.missingConfig).
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun cfg(name: String): String =
        providers.gradleProperty(name).orNull ?: System.getenv(name) ?: localProps.getProperty(name) ?: ""
    fun q(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    buildTypes {
        debug {
            buildConfigField("String", "SUPABASE_URL", q(cfg("SUPABASE_URL").ifBlank { "https://fqluqmrouysbosioqhhg.supabase.co" }))
            buildConfigField("String", "SUPABASE_ANON_KEY", q(cfg("SUPABASE_ANON_KEY").ifBlank { "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZxbHVxbXJvdXlzYm9zaW9xaGhnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg0MjQ2NDcsImV4cCI6MjEwNDAwMDY0N30.j2eCkkVsW6-nko54sQRM9o7eOzfsWzpd-1Hgwg4SlPw" }))
            buildConfigField("String", "API_HOST", q(cfg("TREEFORT_API_HOST").ifBlank { "api.treefort.kids" }))
            buildConfigField("boolean", "DEV_TOOLS", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "SUPABASE_URL", q(cfg("SUPABASE_URL")))
            buildConfigField("String", "SUPABASE_ANON_KEY", q(cfg("SUPABASE_ANON_KEY")))
            buildConfigField("String", "API_HOST", q(cfg("TREEFORT_API_HOST").ifBlank { "api.treefort.kids" }))
            buildConfigField("boolean", "DEV_TOOLS", "false")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Supabase
  implementation("io.github.jan-tennert.supabase:postgrest-kt:2.6.1")
  implementation("io.github.jan-tennert.supabase:gotrue-kt:2.6.1")
  implementation("io.ktor:ktor-client-android:2.3.12")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

  // Phosphor icons and avatars (DESIGN.md section 7)
  implementation("com.adamglin:phosphor-icon:1.0.0")

  // WebRTC & Networking
  implementation("io.getstream:stream-webrtc-android:1.3.10")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  // Push: FCM high-priority data messages wake the app for incoming calls
  implementation("com.google.firebase:firebase-messaging:26.0.0")
}
