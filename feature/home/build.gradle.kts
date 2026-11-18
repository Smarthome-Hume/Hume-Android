plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.smarthome.hume.feature.home"
    // (2026-11-19, BOM 2026.11.01 / Compose 1.13): compileSdk 37 -> 37.1 —
    // cung block AGP 9 nhu :app (minor SDK khong bieu dien duoc bang Int).
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.11.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    // RoomExpandedCards.kt import truc tiep kotlinx.serialization.json
    // (JsonPrimitive/JsonArray/doubleOrNull/contentOrNull) nen module nay
    // phai khai truc tiep, khong an theo :core:model (implementation khong
    // truyen classpath sang module phu thuoc).
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
