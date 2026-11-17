plugins {
    id("com.android.application") version "9.4.1" apply false
    id("com.android.library") version "9.3.1" apply false
    // Baseline Profile Gradle plugin (theo benchmark 1.5.0, 09/2026):
    // version di cung benchmark-macro-junit4:1.5.0. Neu Gradle bao
    // khong giai duoc version nay thi dung 1.4.3 thay the.
    id("androidx.baselineprofile") version "1.5.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
