# Bảng chuẩn hóa font chữ Android (đối chiếu iOS FONT_SPEC.md)

**Ngày:** 2026-10-04 | **Nguồn iOS:** `~/workspace/hume-ios/FONT_SPEC.md` (2026-10-02)
**Quy chuẩn Android:** `MEMORY.md` mục 12–19 — font Montserrat toàn app qua
`MaterialTheme.typography.*`, mọi text đều có fontFamily Montserrat
(`humeTypography()` trong `core/ui/.../theme/HumeM3ETheme.kt`).
**Trạng thái file này:** chỉ lập spec đối chiếu — KHÔNG đổi code font trong task này.

## 1. Token Android hiện có (Montserrat)

| Token | Size | Weight | Ghi chú |
|-------|------|--------|---------|
| displayLarge | 57 | SemiBold 600 | khớp MEMORY "display 57/600" |
| displayMedium | 45 | SemiBold 600 | |
| displaySmall | 36 | SemiBold 600 | == iOS `displaySmall` 36/semibold ✅ |
| headlineLarge | 32 | SemiBold 600 | |
| headlineMedium | 24 | Medium 500 | khớp MEMORY "headline 24/500" |
| headlineSmall | 20 | SemiBold 600 | == iOS `cardTitle` 20/semibold ✅ (token tương đương) |
| titleLarge | 22 | SemiBold 600 | == iOS `titleLarge` 22/semibold ✅ |
| titleMedium | 16 | SemiBold 600 | == iOS `titleMedium` 16/semibold ✅ |
| titleSmall | 14 | SemiBold 600 | |
| bodyLarge | 16 | Medium 500 | |
| bodyMedium | 14 | Medium 500 | == iOS `bodyMedium` 14/medium ✅ |
| bodySmall | 12 | Medium 500 | == iOS `bodySmall` 12/medium ✅ |
| labelLarge | 14 | SemiBold 600 | == iOS `labelLarge` 14/semibold ✅ |
| labelMedium | 12 | SemiBold 600 | |
| labelSmall | 11 | SemiBold 600 | |

> Toàn bộ token Android đã khớp 1:1 với token iOS về size/weight ở các mức
> trùng tên. Chỉ còn việc **dùng đúng token tại đúng vị trí**.

## 2. Bảng mapping vị trí → style Android

| # | Vị trí (theo iOS spec) | iOS style | Android hiện tại | Style Android đề xuất | Trạng thái |
|---|------------------------|-----------|------------------|----------------------|------------|
| 1 | Tiêu đề thẻ: "Điện mặt trời", "Hiệu năng pin", "Năng lượng sử dụng" | cardTitle 20/semibold | `bodyMedium` (14/Medium) + SemiBold — `HomeCards.kt:832`, `BatteryCard.kt:80`, `EnergyConsTab.kt:113` | `headlineSmall` (20/SemiBold) | ❌ CẦN CHỈNH |
| 2 | "Cơ cấu tiêu thụ" | cardTitle 20/semibold | `bodySmall` (12/Medium), màu onSurfaceVariant — `EnergyConsTab.kt:389` | `headlineSmall` (20/SemiBold) | ❌ CẦN CHỈNH (file donut do nhóm khác sở hữu — cần phối hợp) |
| 3 | Tiêu đề section "Phòng" | cardTitle 20/semibold | `M3ESectionTitle`: `titleMedium` (16/SemiBold) + Bold → 16/700 | `headlineSmall` (20/SemiBold) | ⚠️ CẦN QUYẾT ĐỊNH — `M3ESectionTitle` dùng chung toàn app; đổi ảnh hưởng mọi section. Ngoài ra MEMORY 16–17 đã chốt font về SemiBold 600, nên Bold 700 hiện tại cũng lệch quy chuẩn |
| 4 | Tiêu đề subtab Tiêu thụ/Điện mặt trời | cardTitle 20/semibold | Android không có tiêu đề này: header là "Năng lượng" (`headlineMedium` 24/SemiBold) + `EsubGroup` segmented (12.5sp/Bold) | Giữ nguyên thiết kế Android (khác iOS) | ➖ KHÁC THIẾT KẾ — không áp dụng máy móc |
| 5 | Giá trị lớn trong thẻ (số kW, %) | displaySmall 36/semibold | `headlineLarge` 32/SemiBold — solar `HomeCards.kt:842`, cons `EnergyConsTab` | `displaySmall` (36/SemiBold) | ⚠️ GẦN KHỚP — Android 32 vs iOS 36; cân nhắc đồng bộ lên 36 |
| 6 | "Thời gian lớn" thẻ pin (bigTime) | — | `displayMedium` 45/Light — `BatteryCard.kt:134` | Giữ nguyên (cố ý theo "layout ảnh mẫu") | ✅ ĐÃ CHỐT THIẾT KẾ |
| 7 | Nhiệt độ phòng (RoomCard) | titleLarge 22/semibold | `headlineLarge` 32/Light — `HomeRooms.kt` | Quyết định user | ❌ LỆCH LỚN — Android 32/Light là design riêng "như Hume gốc"; đồng bộ iOS = `titleLarge` 22/SemiBold |
| 8 | Nhiệt độ điều hoà (ClimateCard stepper) | (titleLarge 22) | `titleMedium` 16/SemiBold — `HomeRooms.kt:410` | `titleMedium` (16/SemiBold) | ✅ KHỚP token iOS titleMedium |
| 9 | Tên phòng / label chính (RoomCard) | titleMedium 16/semibold | `bodyMedium` (14/Medium) + SemiBold — `HomeRooms.kt` | `titleMedium` (16/SemiBold) | ❌ CẦN CHỈNH |
| 10 | Tên thiết bị (device row) | bodyMedium 14/medium | `bodyMedium` + SemiBold — `HomeRooms.kt:605` | `bodyMedium` (14/Medium) | ⚠️ Android dùng SemiBold thay vì Medium — giữ hay về Medium cần user chốt |
| 11 | Mô tả / đơn vị ("Đang bật · 850 W", "kWh") | bodySmall 12/medium | `bodySmall` + Medium — `HomeRooms.kt:623`, `HomeCards.kt` | `bodySmall` (12/Medium) | ✅ KHỚP |
| 12 | Nút action | labelLarge 14/semibold | (kiểm tra theo từng màn hình khi áp dụng) | `labelLarge` (14/SemiBold) | ✅ token sẵn sàng |
| 13 | Chữ trong pill | bodyMedium 14/medium | Không kiểm tra — `HomePills.kt` do nhóm khác sở hữu | `bodyMedium` (14/Medium) | ➖ NGOÀI PHẠM VI |

## 3. Checklist áp dụng (đề xuất, chưa làm)

- [ ] Card titles (Điện mặt trời / Hiệu năng pin / Năng lượng sử dụng) → `headlineSmall`
- [ ] "Cơ cấu tiêu thụ" → `headlineSmall` (phối hợp nhóm sở hữu file donut)
- [ ] Quyết định: `M3ESectionTitle` ("Phòng" và mọi section) → `headlineSmall` 20/600 hay giữ 16/700
- [ ] Quyết định: nhiệt độ phòng RoomCard 32/Light → `titleLarge` 22/600 (đồng bộ iOS) hay giữ
- [ ] Tên phòng RoomCard 14/600 → `titleMedium` 16/600
- [ ] Giá trị lớn thẻ solar/cons 32/600 → `displaySmall` 36/600
- [ ] Tên thiết bị device row: chốt SemiBold (giữ) hay Medium (theo iOS)
- [ ] Quét `copy(fontSize = ...)` / `TextStyle(` hardcode còn sót (tương đương checklist
      "Xóa mọi `M3EType.custom(size:`" của iOS)

## 4. Ghi chú

- Nguyên tắc iOS "Mọi text PHẢI dùng `M3EType.*`, CẤM hardcode" tương đương Android:
  mọi text PHẢI dùng `MaterialTheme.typography.*`, cấm `fontSize = N.sp` hardcode
  ngoài token (ngoại lệ duy nhất: `EsubGroup` cho phép `fontSize` param vì spec
  khác nhau giữa các tab — đã được user duyệt).
- Không phát hiện chỗ sai font "rõ ràng" tới mức phải sửa ngay trong task này;
  các mục ❌/⚠️ ở trên đều cần user chốt trước khi đổi code (đúng quy tắc task).
