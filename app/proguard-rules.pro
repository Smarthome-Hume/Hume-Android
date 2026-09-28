# --- kotlinx.serialization -------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.smarthome.hume.** {
    *** Companion;
}
-keepclasseswithmembers class com.smarthome.hume.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.smarthome.hume.**$$serializer { *; }

# --- OkHttp ----------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Compose ---------------------------------------------------------------
-dontwarn androidx.compose.**
