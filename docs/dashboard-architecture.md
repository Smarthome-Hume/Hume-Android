# Dashboard Architecture — Entity/UI Decoupling

> Thảo luận và chốt ngày 2026-09-28. Mục tiêu: entity không còn gán chặt vào
> giao diện; user tùy chỉnh sensor và layout ngay trong app, không cần rebuild.

## Vấn đề hiện tại

`entity_id` của Home Assistant nằm cứng trong code UI (Composable). Muốn đổi
sensor, thêm phòng, sắp xếp lại → phải sửa code và build lại app.

## Nguyên tắc: tách 3 lớp

| Lớp | Trách nhiệm | Ví dụ |
|---|---|---|
| **Data** | Entity gì, lấy từ đâu | `sensor.*`, `light.*` từ HA WebSocket/REST |
| **Binding** | Entity nào gắn vào vị trí nào | "Nhiệt độ Phòng Khách" → entity X |
| **UI** | Hiển thị ra sao | Card nhiệt độ, gauge, toggle... |

Config (binding + layout) là **data** (JSON trong DataStore/Room), không phải code.

## Tầng 1 — Query động thay vì hardcode ID (làm trước)

Định nghĩa slot theo đặc tính, không theo ID:

```kotlin
// "nhiệt độ phòng khách" = sensor có device_class=temperature trong area Phòng Khách
repo.getSensors(deviceClass = "temperature", area = "Phòng Khách")
```

- Dùng HA entity registry (`/api/config/entity_registry/list`): có sẵn
  `area_id`, `device_class`, `domain`.
- Thêm sensor mới trong HA → app tự thấy, không sửa code.
- Giải quyết ~80% nhu cầu "đổi sensor".

Domain model gợi ý (`core/model/`):

```kotlin
sealed interface HomeEntity {
  val entityId: String
  val areaId: String?
  data class Sensor(...) : HomeEntity
  data class BinarySensor(...) : HomeEntity
  data class Light(...) : HomeEntity
  data class Climate(...) : HomeEntity
  // ...
}
```

`EntityRepository` trả về domain objects; UI không bao giờ thấy `entity_id` thô.

## Tầng 2 — Card system (tùy chỉnh trong app)

Kiến trúc theo mô hình Lovelace của HA:

```kotlin
data class DashboardCard(
  val id: String,
  val type: CardType,        // SENSOR, TOGGLE, GAUGE, CHART, CAMERA...
  val title: String,
  val entityId: String,      // user đổi được trong app
  val config: Map<String, Any> // unit, ngưỡng màu, ...
)

enum class CardType { SENSOR, TOGGLE, GAUGE, CHART, CAMERA, CLIMATE, SCENE }
```

- Một `CardRenderer` duy nhất đọc list cards → vẽ UI. Thêm loại card mới
  = thêm 1 branch, không đụng các màn hình cũ.
- List cards lưu JSON trong DataStore/Room, theo từng dashboard/phòng.
- Màn "Sửa dashboard" trong app: thêm card → **entity picker**
  (search trong entities, filter theo domain/device_class) → chọn entity →
  chỉnh title/config → xong.
- Card layer cũng là chỗ duy nhất xử lý state bẩn (unavailable, unknown,
  giá trị vô lý như nhiệt độ -2°) → hiện "không có dữ liệu" thay vì số sai.

## Tầng 3 — Đọc Lovelace từ HA (đường dài, optional)

- HA expose `/api/lovelace/dashboard/{id}` → trả về config dashboard user
  đã xếp trên web HA.
- App parse và render native → user tùy chỉnh **trong HA**, app tự theo,
  khỏi build editor riêng.
- Nhược điểm: phải map đủ các loại card của Lovelace.

## Lộ trình (không rewrite)

1. Tạo `EntityRepository` trả domain objects; thay chỗ hardcode bằng query
   theo area/device_class.
2. Rút các card hiện tại (solar, pin, phòng) thành `DashboardCard` +
   renderer chung.
3. Thêm màn edit dashboard + entity picker.
4. (Optional) Sync với Lovelace.

## Ghi chú UI

- App đang ở Material 3 (branch `feat/material3-home`).
- Hướng redesign: **Material 3 Expressive** (xem quyết định ngày 2026-09-28) —
  không dùng Liquid Glass (ngôn ngữ của Apple/iOS 26).
- Icon: **Material Symbols** (flat, variable font) — KHÔNG dùng emoji.
  Dùng trục FILL 0/1 cho trạng thái tắt/bật (vd: bóng đèn rỗng → đặc khi bật).
  Trong Compose: dùng `material-icons-extended` hoặc load font variable.
