plugins {
    id("com.android.application")
    id("androidx.baselineprofile")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.smarthome.hume"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.smarthome.hume"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }

    buildTypes {
        // Benchmark: ban sao cua release (minify + khong debuggable) nhung ky
        // bang debug key de Macrobenchmark cai duoc len may that ma khong can
        // keystore release (theo doc Macrobenchmark). Module :baselineprofile
        // nham toi variant nay qua testBuildType = "benchmark".
        // baselineProfile.automaticGenerationDuringBuild: tuy chon theo doc
        // Macrobenchmark hien tai; neu Gradle sync bao khong co property nay
        // thi xoa dong do, khong anh huong do frame time.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            baselineProfile.automaticGenerationDuringBuild = true
        }
        release {
            // R8 + rut gon tai nguyen: APK release nho hon dang ke va bo bot
            // lop khong dung (vi du phan icon Material chua kip go).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Montserrat qua Google Fonts (downloadable fonts) - khong can chep .ttf vao res/font.
    // KHONG ghim version: BOM 2026.09.00 quan ly artifact nay ( -> 1.12.1).
    // Pin cu 1.7.6 da bi go o Batch 1 vi no ep ui-text 1.7.6 chay cung
    // Compose 1.12.1 (khong tuong thich nhi phan).
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // EncryptedSharedPreferences (Android Keystore) cho HA token.
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Brief sang: worker 6:00 hang ngay + vi tri tho cho thoi tiet.
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    // Macrobenchmark can ProfilerInstaller >= 1.3 de chup/reset profile va
    // xoa shader cache khi do (theo doc Macrobenchmark).
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    // Baseline Profile sinh tu module :baselineprofile, dong goi vao APK
    // release (assets/dexopt/baseline.prof) de ART AOT-compile truoc.
    baselineProfile(project(":baselineprofile"))
    // M3E modules (nhanh feat/m3e-dashboard): UI + data tach theo Clean Architecture.
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:home"))
    implementation(project(":feature:energy"))
    implementation(project(":feature:security"))
    implementation(project(":feature:me"))
    implementation(project(":feature:auth"))
    debugImplementation("androidx.compose.ui:ui-tooling")
}
