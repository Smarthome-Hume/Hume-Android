# So sánh tính năng: Hume (app thật) ↔ Demo M3E (HTML)

> Nguồn: kiểm kê code Hume-Android nhánh `feat/material3-home` (47 file Kotlin)
> đối chiếu với `hume-m3e-v4-dashboard.html` rev12.
> Ký hiệu: **[CÓ]** Hume đã nối HA thật · **[THIẾU]** demo chưa có · **[MỚI]** nâng cấp vượt Hume.

## 1. Trang Nhà

| # | Tính năng Hume | Demo M3E | Kế hoạch port |
|---|---|---|---|
| 1 | Header: tên/avatar từ `person.hutchet`, trạng thái kết nối HA | Chỉ chip tìm kiếm + chuông | **[THIẾU]** → thêm avatar + chấm online, lời chào theo giờ |
| 2 | Chip An ninh: `alarm_control_panel` + 4 chế độ, gọi `alarm_arm_*` kèm **PIN** | 4 thẻ chế độ, tự đóng sau 1s, chưa gọi HA | **[THIẾU]** → nối service thật + dialog nhập PIN |
| 3 | Chip bóng đèn: đếm đèn đang sáng (managed list 24 entity) | Có thẻ đếm, số demo | **[THIẾU]** → nối HA + sheet "Đèn đang sáng" (tắt nhanh từng đèn) |
| 4 | Biểu đồ Điện mặt trời 7 ngày (history HA, refresh 5') | Có chart, số demo cứng | **[THIẾU]** → nối `HistoryFetcher`, giữ style pill 30px |
| 5 | Sparkline "Đang phát · kW" realtime | Có, số demo | **[THIẾU]** → nối sensor PV power live |
| 6 | Pin: SOC + công suất + ngưỡng dự trữ (20%) + thời gian còn/đầy | Có battery bar (flat+wavy đã chốt), số demo | **[THIẾU]** → nối sensor Solis + logic reserve=min(soc,20) |
| 7 | 8 phòng + thiết bị (RoomBubbleConfig), toggle HA thật | Có 8 thẻ compact, fill theo `r.light` | **[THIẾU]** → nối HA, giữ nguyên tắc fill theo đèn |
| 8 | Sheet phòng: nhiệt/ẩm + sparkline 24h (history) | Có tiles nhiệt/ẩm, chưa sparkline | **[THIẾU]** → thêm sparkline 24h |
| 9 | Popup RGB (2 đèn) + nhiệt độ màu + 8 preset | Chưa có | **[THIẾU]** → color picker M3E mới |
| 10 | Popup điều hòa: stepper 16–31°C + 5 mode | Có (stepper + 4 mode + nút nguồn) | Nối `climate.set_temperature` / `set_hvac_mode` |
| 11 | Sheet thông báo (managed sensor list, "x phút trước") | Có popup chuông, nội dung demo | **[THIẾU]** → nối managed list + `last_changed` |
| 12 | Dialog biểu đồ lịch sử 24h (6 sensor, trung bình giờ) | Chưa có | **[THIẾU]** → port, giữ style M3E |
| 13 | Tìm kiếm thiết bị (chip tròn) | Có search view lọc thiết bị | Nối danh sách entity thật |

## 2. Trang Điện (2 subtab: Tiêu thụ / Điện mặt trời)

| # | Tính năng Hume | Demo M3E | Kế hoạch port |
|---|---|---|---|
| 14 | Biểu đồ 7 ngày `energy_home_daily` + chi phí (grid_cost, home_cost, giá EVN) | Có chart + thẻ cost (số demo) | **[THIẾU]** → nối sensor + tính VND local |
| 15 | Công suất realtime: pin / PV / aptomat tổng / nhà | Flow card có 4 node (số demo) | **[THIẾU]** → nối live + đảo chiều pin Sạc/Xả |
| 16 | PV1/PV2 + CB1/CB2/CB3 (segment bar) | Có (chú thích trên bar, rev12) | **[THIẾU]** → nối sensor từng string/aptomat |
| 17 | Danh sách thiết bị: toggle Công suất/Năng lượng, top 5, VND | Có connected button group (số demo) | **[THIẾU]** → nối HA Label "New" + history |
| 18 | "Pin yếu": sensor `*_battery` < 70% (đỏ < 20%) | Chưa có | **[THIẾU]** → thẻ mới |
| 19 | Dòng điện Sáng/Chiều/Tối + 3 aptomat T1/T2/T3 | Chưa có (demo chỉ có CB trong flow) | **[THIẾU]** → thẻ phân bổ tải |
| 20 | Sơ đồ Sunsynk 5 node + inverter (Canvas, neon dot) | Thay bằng flow card M3E 4 node | Giữ flow card M3E (đã chốt), nối HA |
| 21 | Điều khiển sạc/xả Solis (`number`/`switch`/`time`) | Chưa có | **[MỚI]** → nối `number.set_value` (Hume còn chưa nối UI) |

## 3. Trang An ninh

| # | Tính năng Hume | Demo M3E | Kế hoạch port |
|---|---|---|---|
| 22 | Camera Frigate: snapshot 3s, events, tải clip về local, phát offline | Có carousel (ảnh demo) | **[THIẾU]** → FrigateStore + ExoPlayer (đã có lib) |
| 23 | 6 cảm biến cửa + 6 chuyển động + khói + rò nước (trạng thái + giờ đổi) | Có thẻ sensor (số demo) | **[THIẾU]** → nối `binary_sensor` + hiệu ứng cảnh báo |
| 24 | Arm/disarm qua chip (mục 2) | — | Như mục 2 |

## 4. Trang Tôi / Hệ thống

| # | Tính năng Hume | Demo M3E | Kế hoạch port |
|---|---|---|---|
| 25 | Đăng nhập: URL + Long-Lived Token (EncryptedSharedPreferences) | Chưa có | **[THIẾU]** → màn hình login M3E (bắt buộc cho app thật) |
| 26 | Profile: person, email/SĐT local, vị trí, connection pill, đăng xuất | Có theme seeds + dark mode | **[THIẾU]** → thêm profile + connection |
| 27 | Quản lý thiết bị (SensorsSheet: toàn bộ entity, tìm/lọc/ẩn/đổi tên local) | Danh sách thiết bị đơn giản | **[THIẾU]** → port đầy đủ |
| 28 | 8 theme seeds + dark mode (tonal spot) | Có (đã chốt) | Đã port sang `:core:ui` ✅ |

## 5. Nâng cấp vượt Hume (chưa từng có)

| # | Nâng cấp | Ghi chú |
|---|---|---|
| 29 | **[MỚI]** Entity decoupling: binding entity↔UI là data (DataStore), query theo `device_class`+`area` — bỏ hardcode ID dần | Theo `docs/dashboard-architecture.md` |
| 30 | **[MỚI]** Rèm (`cover`) — repo đã có `cover.set_cover_position`, chưa có UI | Thêm vào sheet phòng có rèm |
| 31 | **[MỚI]** Khóa (`lock`) — chưa có trong Hume | Thêm điều khiển khóa cửa |
| 32 | **[MỚI]** Scene/Kịch bản (khối cũ đã bị xóa khỏi Hume) | Làm lại theo phong cách M3E |
| 33 | **[MỚI]** Điều khiển sạc Solis qua `number.set_value` (mục 21) | Hume có hàm, chưa có UI |

## Thứ tự triển khai (đề xuất)

1. `:core:ui` theme + component (đang làm) → `:core:data` repository.
2. Login (25) → Home (1–13) → Điện (14–21) → An ninh (22–24) → Tôi (26–28).
3. Nâng cấp mới (29–33) sau khi 4 tab chạy ổn trên S26 Ultra.
