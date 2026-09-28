# Hume M3E — Kiến trúc dự án (nhánh `feat/m3e-dashboard`)

> Mục tiêu: app Android với giao diện M3 Expressive như bản demo HTML
> (`hume-m3e-v4-dashboard.html`), đầy đủ tính năng Hume, nối Home Assistant thật.
> Tài liệu này chốt **khung dự án** — code UI từng màn hình triển khai theo sau.

## 1. Mô hình tổng thể

```
┌─────────────────────────────────────────────────────────┐
│ UI (Compose + M3 Expressive)  :feature:home/energy/...  │  ← giao diện như demo HTML
├─────────────────────────────────────────────────────────┤
│ ViewModel + UseCase           :feature:*/...            │  ← MVVM, state-hoisting
├─────────────────────────────────────────────────────────┤
│ Repository (domain model)     :core:data                │  ← HomeEntity, không lộ entity_id
├─────────────────────────────────────────────────────────┤
│ DataSource                                          │
│  • HA WebSocket (realtime)    :core:network             │  ← states, events, call_service
│  • HA REST (history/config)   :core:network             │  ← /api/history, entity registry
│  • Frigate (camera)           :core:network             │
│  • DataStore/Room (local)     :core:datastore           │  ← theme, binding, cache
└─────────────────────────────────────────────────────────┘
```

Kế thừa và mở rộng `docs/dashboard-architecture.md` (entity/UI decoupling):
UI không bao giờ thấy `entity_id` thô — binding entity↔vị trí là **data** (DataStore),
query theo đặc tính (`device_class` + `area`), không hardcode ID.

## 2. Module Gradle

```
:app                        — MainActivity, NavHost, DI graph (Hilt)
:core:ui                    — M3E theme: 8 seeds × sáng/tối, shape, type (Montserrat),
                              motion tokens, component dùng chung
                              (M3ECard, M3EPill, WavyBatteryBar, SegmentBar, …)
:core:model                 — domain model thuần Kotlin (HomeEntity sealed interface…)
:core:data                  — repository + usecase (không phụ thuộc Android UI)
:core:network               — HA WebSocket client, HA REST, Frigate client
:core:datastore             — SettingsStore (theme/mode), BindingStore, cache Room
:feature:home               — Trang Nhà (demo: page-home)
:feature:energy              — Trang Điện (demo: page-dien + 2 subtab)
:feature:security            — Trang An ninh (demo: page-security)
:feature:me                  — Trang Tôi (demo: page-me: theme, thiết bị, cài đặt)
```

Quy tắc biên: `feature → core`, không bao giờ ngược lại; `feature` không gọi
trực tiếp `network`.

## 3. Home Assistant — 2 kênh

| Kênh | Dùng cho | Ghi chú |
|---|---|---|
| WebSocket `/api/websocket` | state realtime, subscribe events, `call_service` | đã có `HomeAssistantRepository` (882 dòng) — tái dùng |
| REST `/api/*` | history (`HistoryFetcher` đã có), entity/device/area registry | query động theo `device_class`+`area` |

- Token: EncryptedSharedPreferences (đã có), không log, không commit.
- Mất mạng: UI hiện cache Room + trạng thái offline; reconnect exponential backoff.
- Frigate: clip/snapshot qua `FrigateStore` (đã có) cho thẻ camera.

## 4. Design system M3E (port từ demo HTML)

- **8 seeds** (cam mặc định, green, blue, violet, red, pink, teal, amber) × **sáng/tối** —
  token JSON: `goals/hume-android-app-code-updates/hidden_files/m3e-port/m3e_theme_tokens.json`.
  Đổi seed = đổi cả primary family + neutrals (đúng tonal spot), không chỉ accent.
- **Shape expressive**: card 28–44dp (không dùng 12dp baseline), sheet 44dp top,
  menu 24dp, nút pill; press morph giảm 1 nấc (cấm nhảy pill→chữ nhật).
- **Icon**: Material Symbols Rounded **outlined-only** tuyệt đối; trạng thái bật =
  nền container + chữ đậm, không filled icon.
- **Motion**: vào = emphasized decelerate, ra = accelerate, tương tác = spring
  overshoot; opacity không overshoot.
- **Tonal layering**: surface (nền) → surfaceHighest (thẻ) → surfaceContainer (menu/
  dialog/snackbar) + shadow → navbar surfaceLowest 82% + blur.
- **Type**: Montserrat; display 57/700, headline 24/500.
- **Battery bar** (đã chốt với user): 1 thanh 2 segment — Dự trữ flat 10px +
  Sử dụng wavy 14px (bước sóng 15, biên 4, stroke 6), khe 4px, track 10px,
  stop dot 4px; màu `primary` theo theme. Compose: vẽ bằng Canvas (custom
  `WavyBatteryBar`), không dùng drawable tĩnh.

## 5. Lộ trình triển khai

1. ✅ Nhánh `feat/m3e-dashboard`, kiến trúc này, tokens JSON.
2. `:core:ui` — theme + component M3E dùng chung.
3. `:core:data` — domain model + repository theo entity decoupling.
4. `feature:home` → `feature:energy` → `feature:security` → `feature:me`.
5. Mỗi màn hình: nối HA thật, bỏ mock dần; user build Android Studio + test S26 Ultra.

## 6. So sánh tính năng Hume ↔ demo M3E

(Xem `docs/m3e-feature-gaps.md` — liệt kê chi tiết từng tính năng Hume có mà
demo chưa có, và kế hoạch nâng cấp.)
