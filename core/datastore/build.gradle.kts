plugins {
    id("com.android.library")
}

android {
    namespace = "com.smarthome.hume.core.datastore"
    // (2026-11-19, BOM 2026.11.01 / Compose 1.13): compileSdk 37 -> 37.1 —
    // cung block AGP 9 nhu :app (minor SDK khong bieu dien duoc bang Int).
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(project(":core:model"))
}
