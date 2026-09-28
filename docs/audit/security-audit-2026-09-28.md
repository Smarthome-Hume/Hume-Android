# AUDIT tab An ninh — HTML rev12 vs Android (feat/m3e-dashboard)

Ngày: 2026-09-29. Phạm vi: `id="page-security"` trong `hume-m3e-v4-dashboard.html`
đối chiếu `feature/security/.../SecurityScreen.kt` (696 dòng) + `SecurityViewModel.kt`
+ `core/ui/.../EsubGroup.kt`. Ảnh ref: `hume-html-ref/08-security.png`.
Quy ước: px = dp/sp 1:1. **Không sửa code.**

> Lưu ý cascade: file HTML có các rule cũ (khu vực `.camfeed`, comment
> "Camera + FloatingToolbar (M3E)") vẫn còn hiệu lực vì selector không scope
> (`.ftoolbar`, `.ftbtn`, `.live`, `.dvsegi` viết trần). Rule mới (rev12) ghi đè
> từng property. Dưới đây ghi giá trị **hiệu lực sau cascade**.

---

## 0. Page container

**HTML:** `.page{padding:4px 18px 170px}`
**Android** (`SecurityScreen.kt:118-119`): `.padding(horizontal = 16.dp).padding(bottom = 96.dp)` (+ Scaffold inner padding từ `M3ERootScreen.kt:92`)

- **M1:** padding ngang 16dp vs 18px (thiếu 2dp mỗi bên). Padding đáy 96dp vs 170px (cộng thêm Scaffold inner; cần đối chiếu visual với navbar floating).

## 1. Header `.phdr`

**HTML:**
`.phdr{padding:12px 2px 6px}` · `.phdr h2{font-size:26px;font-weight:700;letter-spacing:-.3px}` ·
`.phdr p{font-size:13px;color:var(--onSurfaceVariant);margin-top:3px}` · rise không delay
**Android** (`SecurityScreen.kt:122-143`): padding start/end 2dp, top 12dp, bottom 6dp; h2 26sp/Bold/ls -.3sp; p 13sp/onSurfaceVariant; Spacer 3dp; `riseIn(0)`.
→ **KHỚP.**

## 2. Camera picker `.esub` / `.dvsegi` → `EsubGroup.kt`

**HTML (hiệu lực):**
`.esub{display:flex;background:var(--surfaceContainer);border-radius:999px;padding:4px;gap:2px;margin-bottom:14px}`
`.dvsegi{flex:1;display:flex;align-items:center;justify-content:center;gap:6px;border:0;background:transparent;color:var(--onSurfaceVariant);font-family:inherit;font-size:12px;font-weight:600;padding:8px 10px;border-radius:999px;cursor:pointer;white-space:nowrap;transition:background .35s var(--ease),color .3s,transform .3s var(--spring)}`
`.dvsegi .ms{font-size:16px;width:0;opacity:0;transform:scale(.4);overflow:hidden;transition:width .3s var(--ease),opacity .25s,transform .35s var(--spring)}`
`.dvsegi.on{background:var(--primaryContainer);color:var(--onPrimaryContainer)}`
`.dvsegi.on .ms{width:16px;opacity:1;transform:scale(1)}`

**Android:** pill surfaceContainer, padding 4dp, gap 2dp ✓; selected primaryContainer/onPrimaryContainer ✓; Spacer 14dp sau group ✓ (= margin-bottom:14px); riseIn(420) ✓ (= .42s).

- **M2 (CRITICAL — compile error):** `EsubGroup.kt:73` dùng `Icon(M3EIcons.Check, ...)` — `M3EIcons.Check` hiện là `String` (glyph PUA), `androidx.compose.material3.Icon()` không có overload String → **type mismatch, cả module không compile**. File này nằm ngoài phạm vi codemod MsIcon (chỉ quét các feature). Cả tab An ninh không build được từ lỗi này.
- **M3:** font-size 12px / weight 600 / padding vertical 8px → Android 12.5sp / Bold(700) / vertical 10dp (`EsubGroup.kt:62,84-85`).
- **M4:** icon check 16px → Android 17dp (`EsubGroup.kt:79`).
- **M5:** `transition:background .35s, color .3s` khi đổi tab → Android chuyển màu nền/chữ instant, không animate.
- **M6 (minor):** `<button>` HTML không ripple → Android `.clickable{}` mặc định có ripple.

## 3. Thẻ camera `.seccam` / `.scfeed`

**HTML:**
`.seccam{background:var(--surfaceHighest);border:0;border-radius:32px;padding:12px}`
`.scfeed{position:relative;border-radius:22px;overflow:hidden;aspect-ratio:16/9;cursor:pointer;background:linear-gradient(135deg,#2b3a4a,#1a2430 60%,#24303d)}`
`.scfeed::before` = 2 radial highlight (120px@20%/30% alpha .09; 200px@75%/70% alpha .06)
`.scview{position:absolute;inset:0;...color:rgba(255,255,255,.35)}` · `.scview .ms{font-size:64px}` ·
`.scfeed.locked .scview{filter:blur(16px) brightness(.8)}`

**Android** (`SecurityScreen.kt:241-330`): M3ECard radius 32dp, padding 12dp, `surfaceContainerHighest` (= `--surfaceHighest`: #FFFFFF light / #35302A dark ✓), elevation 0 ✓; feed radius 22dp, aspect 16/9 ✓; gradient 3 màu ✓; 2 radial qua drawBehind (120dp@20%/30%/.09, 200dp@75%/70%/.06) ✓; locked → blur 16dp + dim đen 20% ✓ (≈ brightness(.8)).

- **M7:** placeholder `<span class="ms">videocam</span>` 64px luôn render trong HTML → Android **không có** (feed trống khi ảnh chưa load).
- **M8 (minor):** text "Không lấy được hình từ Frigate" là phần tử Android tự thêm, HTML không có.
- (minor) gradient: không đúng hướng 135deg và stop 60%.

## 4. Badge LIVE

**HTML (hiệu lực):**
`.scfeed .live{position:absolute;top:10px;left:10px;background:#E53935;color:#fff;font-size:10px;font-weight:800;letter-spacing:1px;padding:5px 10px;border-radius:8px;display:flex;align-items:center;gap:6px}`
`.scfeed .live::before{content:"";width:7px;height:7px;border-radius:50%;background:#fff;animation:blink 1.4s infinite}` (`@keyframes blink{50%{opacity:.25}}`)

**Android** (`SecurityScreen.kt:333-361`): TopStart + padding 10dp ✓; bg #E53935 ✓; radius 8dp ✓; padding 10/5dp ✓; 10sp/ExtraBold(800)/ls 1sp ✓; dot 7dp trắng + `blink(1400)` ✓; gap 6dp ✓.
→ **KHỚP.**

## 5. Tên camera `.scname`

**HTML:** `.scname{position:absolute;top:10px;right:10px;background:rgba(0,0,0,.5);color:#fff;font-size:11.5px;font-weight:700;padding:6px 12px;border-radius:999px;backdrop-filter:blur(6px)}`
**Android** (`SecurityScreen.kt:363-374`): TopEnd + padding 10dp ✓; đen 50% ✓; 11.5sp/Bold ✓; padding 12/6dp ✓; CircleShape ✓.
→ **KHỚP**, trừ backdrop-blur(6px) không implement (code đã ghi chú giới hạn — minor).

## 6. Overlay mở khoá `.scunlock`

**HTML:**
`.scunlock{position:absolute;inset:0;border:0;background:rgba(0,0,0,.35);color:#fff;display:flex;flex-direction:column;gap:8px;align-items:center;justify-content:center;cursor:pointer;font-size:13px;font-weight:600;transition:opacity .4s}`
`.scunlock .ms{font-size:34px}` · `.scfeed:not(.locked) .scunlock{opacity:0;pointer-events:none}`

**Android** (`SecurityScreen.kt:388-410`): Column fillMaxSize, đen 35% ✓; icon lock 34dp ✓; gap 8dp ✓; text 13sp/SemiBold(600) trắng ✓; chỉ hiện khi `!unlocked` ✓.
→ **KHỚP.**

## 7. Toolbar camera `.ftoolbar` / `.ftbtn`

**HTML (hiệu lực sau cascade — rule cũ + rule mới merge):**
`.ftoolbar{position:absolute;bottom:12px;left:50%;transform:translateX(-50%);display:flex;gap:8px;justify-content:center;background:color-mix(in srgb, var(--surfaceLowest) 72%, transparent);-webkit-backdrop-filter:blur(18px);backdrop-filter:blur(18px);border:1px solid rgba(255,255,255,.25);border-radius:26px;padding:12px 4px 4px;box-shadow:var(--shadow)}`
`.ftbtn{width:46px;height:46px;border-radius:50%;border:0;background:var(--surfaceContainer);color:var(--onSurfaceVariant);cursor:pointer;display:flex;align-items:center;justify-content:center;transition:transform .25s var(--spring)}`
`.ftbtn .ms{font-size:22px}` · `.ftbtn:active{transform:scale(.85);background:var(--primaryContainer);border-radius:15px}` · `.ftbtn.rec{color:#E53935}`

**Android** (`SecurityScreen.kt:383-425`): overlay trong Box feed, BottomCenter + padding bottom 12dp (tương đương vị trí overlay theo ảnh ref); radius 26dp ✓; viền trắng 25% 1dp ✓.

- **M9:** padding pill `12px 4px 4px` → Android `.padding(5.dp)` (`:392`).
- **M10:** gap 8px → Android `spacedBy(2.dp)` (`:393`).
- **M11:** nền `surfaceLowest` 72% → Android `surfaceContainerLowest` 72% (`:389`) — sai token.
- **M12:** `backdrop-filter:blur(18px)` → Android không implement.
- **M13:** `box-shadow:var(--shadow)` → Android không có shadow.
- **M14:** nút 46×46px → Android 48dp (`:441`).
- **M15:** radius 50% (tròn) → Android 22dp (`:446` + `ToolbarBtn`).
- **M16:** nền `surfaceContainer` → Android transparent (`:435`).
- **M17:** chữ/icon `onSurfaceVariant` → Android `onSurface` cho mic/photo/fullscreen (`:402-404`).
- **M18:** icon 22px → Android 24dp (`:459`).
- **M19:** `.ftbtn.rec` màu `#E53935` cố định → Android `cs.error` (= #BA1A1A ở light seed cam, đổi theo seed) (`:399`).
- Press khớp cascade-merge: scale .85 ✓, nền primaryContainer ✓, radius 15px ✓.
- Ghi nhận: `.ftoolbar` absolute nhưng `.seccam` không có position → render strict sẽ lệch; ảnh ref + Android đều đặt overlay đáy feed (visual khớp).

## 8. Section header `.sec`

**HTML:** `.sec{display:flex;align-items:baseline;justify-content:space-between;margin:20px 4px 10px}` + `.sec{margin-bottom:12px}` → margin hiệu lực `20px 4px 12px`
`.sec h3{font-size:16px;font-weight:700;letter-spacing:-.1px;color:var(--onSurface)}`
`.secmore{font-size:12px;font-weight:700;color:var(--primary)}`

**Android** (`SecHeader`, `SecurityScreen.kt:196-213`): padding top 20 / bottom 10 / ngang 4dp; SpaceBetween ✓.

- **M20:** margin-bottom 12px → Android bottom 10dp (`:198`).
- **M21:** h3 ls `-.1px` → Android `titleMedium`+Bold giữ letterSpacing mặc định (+0.15) (`:204`).
- **M22 (minor):** `.secmore` ls 0 → Android `labelMedium`+Bold giữ ls 0.5 (`:208`).
- **M23 (minor):** `align-items:baseline` → Android `CenterVertically`.
- Rise delays khớp: .46s/:460, .5s/:500, .52s/:520, .54s/:540. Action "Tải 10 clip" không clickable ✓ (HTML span).

## 9. Video gần đây `.reclist` / `.rec`

**HTML:**
`.reclist{display:flex;gap:10px;overflow-x:auto;padding:2px 2px 6px;scrollbar-width:none}`
`.rec{flex:0 0 132px;border:0;background:var(--surfaceHighest);border-radius:20px;padding:8px;...}`
`.rec:active{transform:scale(.94)}`
`.recthumb{...aspect-ratio:150/86;border-radius:13px;background:linear-gradient(135deg,#33414f,#1c2530);color:rgba(255,255,255,.75)}`
`.recthumb .ms{font-size:34px}` · `.reclab{display:block;font-size:11px;font-weight:700;color:var(--onSurface);margin-top:7px;line-height:1.5}`
Item JS: `<button class="rec"><span class="recthumb"><span class="ms">play_circle</span></span><span class="reclab">09:41 · 28/09</span></button>` × 6 clip.

**Android** (`RecCard`, `:468-500`; `LazyRow :179-187`): LazyRow gap 10dp ✓; card 132dp ✓, surfaceContainerHighest ✓, radius 20dp ✓, padding 8dp ✓, pressMorph .94 ✓; thumb 150/86 ✓, radius 13dp ✓, gradient ✓, icon play_circle 34dp trắng 75% ✓; label 11sp/Bold/margin-top 7dp ✓.

- **M24:** `.reclist` padding `2px 2px 6px` → Android LazyRow không có contentPadding.
- (info) số clip: HTML demo 6; Android lấy từ repo theo camera (data-dependent).

## 10. Cảm biến `.sgrid` / `.scard`

**HTML:**
`.sgrid{display:grid;grid-template-columns:1fr 1fr;gap:10px}`
`.scard{background:var(--surfaceHighest);border:0;border-radius:26px;padding:14px;position:relative;overflow:hidden;transition:background .3s,box-shadow .3s}`
`.scard .sic{width:44px;height:44px;border-radius:22px;background:var(--surfaceContainer);color:var(--onSurfaceVariant);display:flex;align-items:center;justify-content:center;margin-bottom:10px;transition:background .3s,color .3s}`
`.scard .sic .ms{font-size:24px}`
`.scard .srow{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:2px}`
`.scard .stx{flex:1;min-width:0}`
`.scard .snm{font-size:13px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}`
`.scard .stm{font-size:11px;color:var(--onSurfaceVariant);font-weight:500;margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}`
`.scard .sst{display:inline-block;flex:none;margin-top:0;font-size:10.5px;font-weight:800;letter-spacing:.8px;padding:6px 12px;border-radius:999px;background:var(--surfaceContainer);color:var(--onSurfaceVariant)}`
`.scard.on{background:var(--errorContainer)}` · `.scard.on .sic{background:rgba(255,255,255,.55);color:var(--onErrorContainer)}` ·
`.scard.on .snm{color:var(--onErrorContainer)}` · `.scard.on .stm{color:var(--onErrorContainer);opacity:.75}` ·
`.scard.on .sst{background:var(--error);color:#fff}` ·
`.scard .neon{position:absolute;top:12px;right:12px;width:9px;height:9px;border-radius:50%;background:var(--error);box-shadow:0 0 10px var(--error);opacity:0;animation:blink 1.2s infinite}` ·
`.scard.on .neon{opacity:1}` ·
`.scard.warn.on{background:var(--tertiaryContainer)}` · `.scard.warn.on .sic{color:var(--onTertiaryContainer)}` ·
`.scard.warn.on .snm{color:var(--onTertiaryContainer)}` · `.scard.warn.on .stm{color:var(--onTertiaryContainer);opacity:.75}`
(Không có rule `.warn.on .sst` → chip vẫn `var(--error)` nền như `.on`.)

**Android** (`SensorCard :524-610`, `SensorGrid :513-527`): radius 26dp ✓, padding 14dp ✓, bg surfaceContainerHighest ✓; sic 44dp/tròn/surfaceContainer/onSurfaceVariant/margin-bottom 10dp/icon 24dp ✓; cấu trúc neon+sic+srow(stx(snm+stm)+sst) ✓; snm 13sp/Bold/ellipsis ✓; stm 11sp/Medium/margin-top 2dp/ellipsis ✓; sst 10.5sp/ExtraBold(800)/ls .8sp/padding 12/6dp/tròn ✓; state on (errorContainer, sic trắng 55%, chữ onErrorContainer, stm alpha .75, chip nền error chữ trắng) ✓; warn.on (tertiaryContainer + onTertiaryContainer) ✓; chip warn.on nền error chữ trắng ✓ (đúng vì không có rule riêng); neon 9dp top/end 12dp + halo blur, blink 1200, chỉ khi on ✓; grid 2 cột gap 10dp (rows weight) ✓; chip labels MỞ/ĐÓNG, PHÁT HIỆN/TRỐNG, BÁO ĐỘNG/Bình thường/An toàn ✓.

- **M25:** `.srow{margin-top:2px}` → Android Row không có margin-top 2dp.
- **M26 (minor):** `transition:background .3s` khi đổi trạng thái → Android đổi bg instant.

## 11. Clip overlay `.clipov`

**HTML:**
`.clipov{position:fixed;inset:0;background:#000;z-index:200;display:none;align-items:center;justify-content:center;flex-direction:column;gap:18px}`
`.clipov.on{display:flex}`
`.cv{width:88%;aspect-ratio:16/9;border-radius:20px;background:linear-gradient(135deg,#2b3a4a,#141c26);display:flex;align-items:center;justify-content:center;color:rgba(255,255,255,.4)}`
`.cv .ms{font-size:72px}`
`.ct{font-size:14px;font-weight:700;margin-bottom:12px}` + `.ct{color:#fff;...}`
`.cx{border:0;background:rgba(255,255,255,.14);color:#fff;font-family:inherit;font-size:13px;font-weight:700;padding:12px 28px;border-radius:999px;cursor:pointer}`
Title JS: `clips[i][0]+' · '+clips[i][1]` ("09:41 · 28/09"); click `.rec` → overlay on; `#clipClose` → off.

**Android** (`ClipOverlay :615-696`): Dialog full-width, nền đen, căn giữa ✓; cv 88%/16:9/radius 20/gradient/icon videocam 72dp trắng 40% ✓; title 14sp/Bold/trắng ✓ ("time · date" ✓); nút Đóng padding 28/12dp, tròn, trắng 14%, 13sp/Bold ✓.

- **M27 (minor):** khoảng title→nút: HTML gap 18px + `.ct{margin-bottom:12px}` = 30px → Android `spacedBy(18.dp)`.
- (minor) nút Đóng: HTML `<button>` không ripple → Android `clickable` mặc định có ripple.

## 12. Behavior

| # | HTML | Android | Kết quả |
|---|------|---------|---------|
| B1 | Click camera: toggle `.on`, `scname`=tên cam, feed re-`locked`, `vibrate(6)` | `selectCamera()`: set index, `_unlocked=false`, `refreshRecordings`, `haptic()` | KHỚP |
| B2 | Click feed → remove `.locked`, `vibrate(8)`; Enter/Space cũng unlock | Box `pressMorph` → `vm.unlock()` + haptic | KHỚP (trừ keyboard) |
| B3 | `setInterval` 22s: random toggle 1 PIR `.on`, chip ↔ PHÁT HIỆN/TRỐNG, stm="Vừa xong" | `SecurityViewModel` loop 22s toggle flicker + "Vừa xong" | KHỚP |
| B4 | Click `.rec` → `.clipov.on` + title "09:41 · 28/09"; Đóng → off | `openClip`/`closeClip` + Dialog | KHỚP |
| B5 | Nút rec: không có JS toggle (chỉ màu đỏ) | `toggleRec()` đổi state nhưng **không UI nào đọc** (dead state) | Thừa, vô hiệu |
| B6 | `scfeed` tabindex=0, keydown Enter/Space | Không xử lý keyboard focus | minor |

## 13. Icon (ligature HTML → Ms.*)

`check`→Ms.check ✓ · `videocam`→Ms.videocam ✓ · `lock`→Ms.lock ✓ ·
`fiber_manual_record`→Ms.fiber_manual_record ✓ · `mic`→Ms.mic ✓ ·
`photo_camera`→Ms.photo_camera ✓ · `fullscreen`→Ms.fullscreen ✓ ·
`play_circle`→Ms.play_circle ✓ · `door_front`→Ms.door_front ✓ ·
`sensors`→Ms.sensors ✓ · `person_search`→Ms.person_search ✓ ·
`smoke_free`→Ms.smoke_free ✓ · `water_drop`→Ms.water_drop ✓.
Tất cả đúng tên; nhưng `EsubGroup.kt:73` gọi qua `Icon()` (material3) thay vì `MsIcon()` → xem M2.

## 14. Motion tokens

`--motion-emphasized:cubic-bezier(.05,.7,.1,1)` = `M3EMotion.emphasized` ✓ ·
`--motion-spring:cubic-bezier(.34,1.45,.5,1)` = `M3EMotion.spring` ✓ ·
`rise .7s` từ `translateY(22px)`, delays .42→.54s ✓ (`riseIn`).
`blink 1.4s` (live dot) / `1.2s` (neon) ✓.

---

## TỔNG: 27 mismatches (M1–M27)

- **Critical (block build): 1** — M2: `EsubGroup.kt:73` `Icon(M3EIcons.Check)` type mismatch (String vs ImageVector), cả tab không compile.
- **Sai số/kích thước: 9** — M1 (padding ngang trang), M3, M4, M9, M10, M14, M15, M18, M20.
- **Sai màu/token: 4** — M11 (surfaceLowest→surfaceContainerLowest), M16 (nút toolbar transparent), M17 (onSurfaceVariant→onSurface), M19 (rec #E53935→cs.error).
- **Thiếu hiệu ứng: 5** — M5, M12 (blur toolbar), M13 (shadow toolbar), M26 (transition bg card), + backdrop-blur `.scname` (ghi nhận).
- **Sai typography: 2** — M21 (ls h3), M22 (ls secmore, minor).
- **Thiếu thành phần: 2** — M7 (placeholder videocam 64px), M24 (contentPadding reclist).
- **Minor khác: 4** — M6, M8, M23, M25, M27 (+ B5 dead state, B6 keyboard).

Khớp hoàn toàn: header `.phdr`, badge LIVE, `.scname`, overlay `.scunlock`, thẻ `.seccam`, card `.rec`, card `.scard` (+states on/warn/neon/chip), clip overlay (trừ M27), toàn bộ behavior B1–B4, icon mapping, motion tokens.
