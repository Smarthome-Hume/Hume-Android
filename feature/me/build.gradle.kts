plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.smarthome.hume.feature.me"
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
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    // MeScreen mo RoomManagementScreen + dung RoomStore/PickerEntity (type
    // cua :feature:home, truyen tu M3ERootScreen o :app). Them dep nay de
    // het 3 loi 'home' unresolved o MeScreen.kt (2026-11-17). Day la dep
    // feature->feature dau tien trong project; chieu 1 chieu, :feature:home
    // khong depend :feature:me nen khong tao vong tron.
    implementation(project(":feature:home"))
}
