# Modernize Android Project Configuration

This plan covers migrating to AGP 9.0's built-in Kotlin support, updating the project to the latest Android SDK and dependency versions, and resolving DSL deprecation warnings.

## User Review Required

> [!IMPORTANT]
> Enabling `android.newDsl=true` and `android.builtInKotlin=true` is a significant change that simplifies the build configuration by removing the manual application of the `kotlin-android` plugin. This is the recommended path for AGP 9.0+.

## Proposed Changes

### Build Configuration & Environment

#### [MODIFY] [gradle.properties](file:///C:/Users/okash/StudioProjects/Hume-Android/gradle.properties)
- Enable built-in Kotlin: `android.builtInKotlin=true`.
- Enable new DSL: `android.newDsl=true`.
- Update `android.sync.suppressAgpWarnings` to `DEPRECATED_DSL`.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/okash/StudioProjects/Hume-Android/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")` from the root plugins block.
- Update Kotlin plugin versions for `serialization` and `compose` to `2.4.10`.

### App Module Improvements

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/okash/StudioProjects/Hume-Android/app/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")` from the plugins block.
- Update `compileSdk` and `targetSdk` to `37`.
- **Dependency Updates**:
    - Update `androidx.core:core-ktx` to `1.19.0`.
    - Update `androidx.activity:activity-compose` to `1.13.0`.
    - Update `androidx.lifecycle` libraries to `2.11.0`.
    - Update `androidx.datastore:datastore-preferences` to `1.2.1`.
    - Update `com.squareup.okhttp3:okhttp` to `5.4.0`.
    - Update `kotlinx.coroutines` and `kotlinx.serialization` to `1.11.0`.
    - Update `androidx.media3` libraries to `1.10.1`.
    - Update `composeBom` to `2026.06.01`.
- **Clean up Dependencies**:
    - Investigate and fix the duplicate `composeBom` declaration (likely by removing the local variable and using the platform dependency directly if redundant).
- **DSL Migration**:
    - Adjust the `android` block to be compatible with `ApplicationExtension` requirements.

## Verification Plan

### Automated Tests
- Perform a Gradle Sync to verify the new DSL and built-in Kotlin configuration.
- Run `./gradlew :app:assembleDebug` to ensure the project compiles with SDK 37 and updated dependencies.

### Manual Verification
- Check for any remaining deprecation warnings in the Build output.
