plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.smarthome.hume.baselineprofile"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.benchmark.macro.junit4.MacrobenchmarkJUnit4Runner"
    }
    // Do tren variant "benchmark" cua app (ban sao release, ky debug key).
    targetProjectPath = ":app"
    testBuildType = "benchmark"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Benchmark 1.5.0 stable (09/09/2026, developer.android.com).
    // Plugin androidx.baselineprofile dung cung version 1.5.0 (khai o root).
    androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
