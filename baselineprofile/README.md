# Baseline Profile + Macrobenchmark — đo hiệu năng Hume Android (Batch 1)

Module này sinh Baseline Profile và đo hiệu năng thật trên máy (không đo
trên emulator, không hứa trước số % cải thiện nào).

## Thành phần

- `BaselineProfileGenerator.kt` — chạy 3 journey (khởi động + cuộn trang
  Nhà, tab Năng lượng, RoomSheet + thẻ mở rộng) để sinh profile.
- `StartupBenchmarks.kt` — đo `timeToInitialDisplay` khi khởi động lạnh,
  so sánh `CompilationMode.None()` với `Partial(BaselineProfileMode.Require)`.
- `ScrollBenchmarks.kt` — đo frame time (`FrameTimingMetric`: frame
  duration, overrun, jank) khi cuộn ở 3 màn nặng nhất: Nhà, Năng lượng,
  thẻ mở rộng trong phòng.
- `Journeys.kt` — các bước đi chung cho cả generator và benchmark
  (tab theo `HumeTab.label`, phòng mặc định, vuốt lên/xuống).

## Điều kiện chạy (bắt buộc)

1. **Máy thật S26 Ultra, đã đăng nhập sẵn vào app.** Chưa đăng nhập thì
   app hiện LoginScreen: generator chỉ thu được startup profile, còn
   các benchmark cuộn tự skip qua `Assume` (không đo số liệu ảo).
2. Máy phải root HOẶC API 33 trở lên để sinh profile trên connected
   device — S26 Ultra (API 37) thỏa điều kiện này.
3. Dùng build variant **`benchmark`** (bản sao của release: minify +
   không debuggable, nhưng ký bằng debug key). Không đo trên bản debug
   (debuggable + không minify cho số khác hẳn bản phát hành) và không
   đo trên emulator.
4. Tắt các app nặng / để máy nguội trước khi đo; chạy mỗi benchmark
   ít nhất 1 lần làm nóng rồi mới lấy số so sánh.

## Lệnh chạy (tại gốc repo, máy S26 Ultra cắm qua ADB)

```bash
# 1. Build variant benchmark (sync Gradle trước trong Android Studio)
./gradlew :app:assembleBenchmark

# 2. Sinh Baseline Profile từ 3 journey (chạy trên máy thật)
./gradlew :app:generateBenchmarkBaselineProfile
# Profile được copy về:
#   app/src/benchmark/generated/baselineProfiles/baseline-prof.txt
# Kiểm tra profile có trong APK release:
#   ./gradlew :app:assembleRelease
#   unzip -l app/build/outputs/apk/release/*.apk | grep baseline.prof
#   (phải thấy assets/dexopt/baseline.prof)

# 3. Đo khởi động lạnh (trước/sau profile)
./gradlew :baselineprofile:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.smarthome.hume.baselineprofile.StartupBenchmarks

# 4. Đo frame time khi cuộn 3 màn nặng nhất
./gradlew :baselineprofile:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.smarthome.hume.baselineprofile.ScrollBenchmarks
```

Hoặc chạy từng test từ Android Studio (gutter ▶ bên cạnh `@Test`).
Kết quả benchmark nằm ở
`baselineprofile/build/outputs/connected_android_test_additional_output/benchmark/`.

## Lưu ý khi số liệu lạ

- `expandedCardScroll` bị skip: máy không thấy phòng mặc định nào
  (đã đổi tên/xóa hết) — sửa `DEFAULT_ROOM_NAMES` trong `Journeys.kt`
  theo tên phòng thật, hoặc sửa tọa độ trong `tapFirstDeviceRow()`
  (hiện bấm thử vào điểm 50% rộng / 40% cao của sheet).
- Benchmark cuộn fail với `BaselineProfileMode.Require`: chưa sinh
  profile — chạy lại bước 2 trước, đây là cố ý (fail to thay vì đo
  lặng lẽ trên bản không có profile).
- Nếu Gradle sync báo không giải được `androidx.baselineprofile`
  version `1.5.0` (khai ở root `build.gradle.kts`), đổi về `1.4.3`.
- Nếu Gradle sync báo không có property
  `baselineProfile.automaticGenerationDuringBuild` trong build type
  `benchmark` (app), xóa dòng đó — tùy chọn, không ảnh hưởng đo.
- Nếu sync báo không biên dịch được file `.kt` trong module này
  (thiếu Kotlin plugin), thêm `id("org.jetbrains.kotlin.android")`
  vào `plugins` của `baselineprofile/build.gradle.kts` và khai
  `id("org.jetbrains.kotlin.android") version "2.4.10" apply false`
  ở root `build.gradle.kts` (dự án đang dùng AGP built-in Kotlin
  qua `android.builtInKotlin=true` nên trường hợp này không nên xảy ra).

## Phiên bản đã dùng (Batch 1, kiểm chứng 17/11/2026)

- Compose BOM `2026.09.00` (Compose core 1.12.1, Material3 1.4.0 —
  developer.android.com: "Always use the latest Compose BOM version:
  2026.09.00").
- Benchmark `androidx.benchmark:benchmark-macro-junit4:1.5.0`
  (stable 09/09/2026, developer.android.com).
- `androidx.test.ext:junit:1.2.1`,
  `androidx.test.uiautomator:uiautomator:2.3.0`,
  `androidx.profileinstaller:profileinstaller:1.3.1` (app).
- compileSdk/targetSdk 37, minSdk 26, AGP 9.4.1/9.3.1, Kotlin 2.4.10.
