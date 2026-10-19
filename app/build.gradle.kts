import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    // Debug screen captures read the shared docs/fixtures/guardian.json (see ScreenFixture).
    sourceSets.getByName("debug").assets.srcDir("../../docs/fixtures")

    namespace = "com.treefort.guardian"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.treefort.guardian"
        minSdk = 24
        targetSdk = 36
        versionCode = 121
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

    // Sign in with Google / Apple (docs/OAUTH_SETUP.md). Off unless the flag is exactly "true".
    // GOOGLE_WEB_CLIENT_ID is the Google *web* OAuth client ID that Supabase's Google provider
    // also lists; no secret is ever compiled in. Same source order as above; never committed.
    defaultConfig {
        buildConfigField("String", "OAUTH_GOOGLE_ENABLED", q(cfg("TREEFORT_OAUTH_GOOGLE_ENABLED")))
        buildConfigField("String", "OAUTH_APPLE_ENABLED", q(cfg("TREEFORT_OAUTH_APPLE_ENABLED")))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", q(cfg("GOOGLE_WEB_CLIENT_ID")))
    }

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

  // Sign in with Google (Credential Manager -> Google ID token -> Supabase signInWith(IDToken))
  implementation("androidx.credentials:credentials:1.5.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

  // Phosphor icons and avatars (DESIGN.md section 7)
  implementation("com.adamglin:phosphor-icon:1.0.0")

  // WebRTC & Networking
  implementation("io.getstream:stream-webrtc-android:1.3.10")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  // Push: FCM high-priority data messages wake the app for incoming calls
  implementation("com.google.firebase:firebase-messaging:26.0.0")
}

// Shared Help/FAQ content: docs/help_content.json in the meta repo is the single source. Copy it
// into generated assets at build time (build/ is gitignored, so there is no committed duplicate).
abstract class CopyHelpContent : DefaultTask() {
    @get:InputFile abstract val source: RegularFileProperty
    @get:OutputDirectory abstract val outDir: DirectoryProperty
    @TaskAction fun run() {
        source.get().asFile.copyTo(outDir.get().file("help_content.json").asFile, overwrite = true)
    }
}
val copyHelpContent by tasks.registering(CopyHelpContent::class) {
    source.set(rootProject.layout.projectDirectory.file("../docs/help_content.json"))
}
androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyHelpContent, CopyHelpContent::outDir)
    }
}
