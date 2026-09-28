# Checklist audit HTML→Android (rev12 → feat/m3e-dashboard)

Ngày: 2026-09-29. Spec: `hume-m3e-v4-dashboard.html` rev12 (2026-09-28).
Trạng thái: **static-checked, chưa build-verified** (sandbox không build được Gradle).

## Đã sửa (commit này)

### Core (`:core:ui`)
- [x] `HumeExtraColors`: thêm `surfaceHighest`, `surfaceLowest` — exact 16 scheme từ HTML
      (light cam = `:root`, dark cam = `body.dark`, 7 seed còn lại × light/dark).
- [x] `rememberNeighborPress(count, pressedWeight, neighborWeight)` + `isNeighbor(i)`.
- [x] `EsubGroup`: params context-specific (Energy 12.5sp/700/pad 10×10/check 17;
      Security 12sp/600/pad 8×10/check 16), press `scale(.94)`.
- [x] `M3ESwitch`: off-track dùng `surfaceHighest`; check icon dùng `MsIcon`.
- [x] `M3EConnectedButtonGroup`: icon callback `ImageVector` → `String`, dùng `MsIcon`.
- [x] Font `material_symbols_rounded.ttf`: 69 glyph (FILL=0, wght=200, GRAD=0, opsz=24),
      thêm `remote_gen` (U+E83E), `snowflake` (U+F16F), `lock_open` (U+E898),
      `door_open` (U+E77C); outline 65 glyph cũ giữ nguyên.

### Tab Nhà (Home)
- [x] Header: avatar 55 + presence dot 16/viền 3 (=22), gap 10, nút search/notif 46
      `surfaceHighest`, badge vị trí đúng.
- [x] Pills: `.pill` `surfaceHighest` bo 28, padding 14, gap 11; pic 44; pl 14/700; ps 12/500.
- [x] SecPill đổi theo mode (shield/flight_takeoff/bedtime/power_settings_new + ps text).
- [x] Expanded: hàng giữ nguyên, scroll-x; secpill min-w 150, bulbPill min-w 128;
      smode 92/bo 26/pad 14×10/icon 24/label 12-700, selected primaryContainer,
      smIn (translateX 18 + scale .9, .45s spring), stagger 60ms, :active scale .92,
      auto-collapse 1000ms sau khi chọn.
- [x] SuggestCard: icon auto_awesome 30 bare, button onTertiaryContainer bo 20,
      done → "Đã bật" + opacity .6 (đúng `.sgbtn:disabled`); nội dung data-driven
      (pin thấp / cửa mở / nắng to), fallback text demo khi không có điều kiện.
- [x] SolarWeekCard: header không icon, maxV=7 cố định, tooltip tap, tick 8s +0.06 trần 6.8.
- [x] BatteryCard: footer "Còn khoảng XhYY" + "Kết thúc lúc HH:MM", tick SOC 2.8s,
      wavy path đúng hình học HTML (M2 7 Q5 -1 8 7 T…), legend reserve/usage.
- [x] RoomGrid: gap 10, card `surfaceHighest`, `.lit` chỉ đổi nền (text giữ nguyên).
- [x] RoomSheet: padding 22, bỏ icon trước title, shsub margin 4/16, env tiles,
      climate clamp 16..30, 4 mode Lạnh/Khô/Quạt/Tự động, bỏ heading "Thiết bị",
      device rows margin 10, snowflake icon thật.
- [x] NotificationSheet: palette npool exact (door primary, motion tertiary,
      light secondary, AC info, lock error, camera violet), icon door_front/door_open/
      sensors/lightbulb/ac_unit/lock_open/videocam, handle + padding đúng.
- [x] Snackbar: left/right 16, bottom 104, vào fade .25 + translateY 16→0 .35,
      ra ngược lại; anatomy surfaceContainerHigh + onSurface + action primary.
- [x] EcoDialog exit/scrim đúng; FAB không đổi.
- [x] Vertical rhythm: margin-collapse (card→card 14, pills→sec 20, sec→card 12).

### Tab Điện (Energy)
- [x] Week bar = % của container 120dp; spacing theo CSS; cards `surfaceHighest`.
- [x] Cost display tick 9s; donut slices không khe; device segments weight 1f.
- [x] `remote_gen` icon; flow seg/SOC width animation; sweep restart khi lệch >12%.
- [x] Solar tier bars normalize theo tổng; mini unit typography; control row gap 12.
- [x] Bỏ press interaction không có trong HTML.

### Tab An ninh (Security)
- [x] Page padding 18, bottom 170; EsubGroup spec security; toolbar pad/gap/size/
      radius/background/icon colors, record red, shadow.
- [x] Cards `surfaceHighest`; section spacing; LazyRow padding; sensor row margin.
- [x] Placeholder `videocam` 64 alpha 35%; bỏ error text Frigate; sensor color transitions.
- [x] Xóa dead recording state/toggle; keyboard Enter/Space trên feed.

### Tab Tôi (Me)
- [x] Page 18/170; header 16/2/6; section bottom 12; SyncCard top 4 bỏ press morph.
- [x] DarkModeRow bottom 4; seed row 2/8; notification weights 1.18/.93 + scaleX(.95).
- [x] "Bỏ qua" chỉ rung (không ẩn card); dark mode mặc định false.
- [x] "Giao diện" có entrance; chữ "và" in đậm; shadow rgba(25,20,18,.10).

### Navbar (M3ERootScreen)
- [x] Nền `surfaceLowest` 82%, gap 0, padding 10, shadow 12, bo 34.
- [x] Item: gap 3, padding 9/7, bo 24, icon 24→27 khi chọn, label 11 bold khi chọn.
- [x] Neighbor-press 1.18/.93.

## Giới hạn nền tảng (ghi nhận, không fix được trong Compose)
- Backdrop blur (navbar blur 22px, toolbar blur) — Compose không có backdrop-filter.
- Blob keyframes exact của tab Tôi — dùng sin approximation.
- Legend stagger 0/200/400 thay vì 0/250/400 (helper hiện tại).
- Glow/blur Canvas chỉ gần CSS, không exact.

## Quyết định giữ lại (khác audit máy móc)
- `navigationBarsPadding()` — máy thật cần.
- Solar tooltip tap-toggle (mobile không hover).
- BulbPill mở LightsSheet (tính năng đang dùng).
- Contact sensor best-effort khi thiếu data.
- Flow card: không bịa data thứ hai nếu model chỉ có `todayKwh`.
- Dark + seed: đủ 8 dark schemes (không cascade).
- HA mode `heat` → fallback "cool" (demo chỉ có 4 mode).

## Chờ user verify (visual proof trên S26 Ultra)
- [ ] Ảnh Android: Home đầu trang, Home expanded security, RoomSheet có AC,
      Điện/Tiêu thụ, Điện/Điện mặt trời, An ninh, Tôi.
- [ ] Đặt cạnh 9 ảnh `~/workspace/hume-html-ref/`.
