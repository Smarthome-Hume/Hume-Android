plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.smarthome.hume.feature.energy"
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
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    // PowerwallViewModel.kt:151 doc HomeEntity.attributes
    // (Map<String, JsonElement> dinh nghia o :core:model) nen module nay
    // phai khai truc tiep, khong an theo :core:model (implementation khong
    // truyen classpath sang module phu thuoc) — neu khong compile gay
    // "Cannot access class 'kotlinx.serialization.json.JsonElement'".
    // Cung cach fix nhu :feature:home (RoomExpandedCards.kt).
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
