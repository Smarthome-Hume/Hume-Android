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
    // (2026-11-17) AGP 9.4.1 BO testBuildType khoi TestExtension — javap
    // tren gradle-api-9.4.1.jar xac nhan chi con get/setTargetProjectPath,
    // khong con testBuildType; dong cu gay "Unresolved reference" khi
    // configure. Da xoa; module nay nham dung variant benchmark khi chay
    // generateBaselineProfile se kiem lai luc chay benchmark that.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Benchmark 1.5.0 stable (09/09/2026, developer.android.com).
    // Plugin androidx.baselineprofile dung cung version 1.5.0 (khai o root).
    // (2026-11-17) Module com.android.test o AGP 9.4.1 KHONG con tao
    // configuration androidTestImplementation (chi AppPlugin/LibraryPlugin
    // co — kiem bang scan string trong gradle-9.4.1.jar); deps cua module
    // test dung implementation. Da doi, `gradlew help` config PASS.
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
