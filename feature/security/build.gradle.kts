plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.smarthome.hume.feature.security"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // ExoPlayer cho ClipOverlay (video ghi hinh that, khop version voi :app = 1.5.1)
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    // FIX crash 2 (16:16:39, PID 31160): LivePlayerOverlay choi HLS (m3u8, go2rtc :1984)
    // nhung thieu artifact HLS -> DefaultMediaSourceFactory.createMediaSource load
    // reflectively androidx.media3.exoplayer.hls.HlsMediaSource$Factory va nem
    // ClassNotFoundException -> IllegalStateException tai SecurityScreen.kt:704.
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
}
