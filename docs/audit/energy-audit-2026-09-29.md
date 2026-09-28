# AUDIT tab ĐIỆN — Android vs HTML rev12 (1-1 copy)

Ngày: 2026-09-29 · Phạm vi: AUDIT ONLY, không sửa code.
HTML spec: `~/workspace/your_files/hume-m3e-v4-dashboard.html` — `#page-dien` (dòng 1052–1190), CSS nội tuyến.
Code: `~/workspace/Hume-Android`, nhánh `feat/m3e-dashboard`.

Cấu trúc sau khi JS di chuyển block (`mv('m-flx','#secEnergy')`, `mv('m-dvx','.dvx')`, donut `.ecard`→`#m-donut`, xoá `.energy` cũ):
- `#etab-cons`: wcard → cost → pwcard → donut "Cơ cấu tiêu thụ" → dvx "Thiết bị tiêu thụ" → blw
- `#etab-solar`: flx (flow card) → syn → exp "Sạc pin" → exp "Xả pin"

Quy ước: px HTML = dp/sp Android 1-1. `Ms.*` = glyph String trong font `material_symbols_rounded.ttf` (wght 200/FILL 0).

---

## 0. LỖI COMPILE — CRITICAL (2)

**C1. `core/ui/.../components/EsubGroup.kt:73` — `Icon(M3EIcons.Check, ...)`**
- `M3EIcons.Check` hiện là `String` (glyph `Ms.check`), `Icon()` cần `ImageVector` → không compile.
- Hệ quả: **cụm chuyển subtab `.esub` (Tiêu thụ / Điện mặt trời) không build được** → cả tab Điện gãy.

**C2. `core/ui/.../components/M3ESwitch.kt:113` — `Icon(imageVector = M3EIcons.Check, ...)`**
- Cùng nguyên nhân: `M3EIcons.Check` là String → không compile.
- Hệ quả: mọi switch trong expander Sạc/Xả pin gãy.

---

## 1. `.esub` — cụm chuyển subtab (EsubGroup.kt)

HTML:
```css
.esub{display:flex;background:var(--surfaceContainer);border-radius:999px;padding:4px;gap:2px;margin-bottom:14px}
.esub .dvsegi{flex:1;display:flex;align-items:center;justify-content:center;gap:6px;border:0;background:transparent;color:var(--onSurfaceVariant);font-family:inherit;font-size:12.5px;font-weight:700;padding:10px 10px;border-radius:999px;cursor:pointer;white-space:nowrap;transition:background .35s var(--ease),color .3s,transform .3s var(--spring)}
.esub .dvsegi .ms{font-size:17px;width:0;opacity:0;transform:scale(.4);overflow:hidden;transition:width .3s var(--ease),opacity .25s,transform .35s var(--spring)}
.esub .dvsegi.on{background:var(--primaryContainer);color:var(--onPrimaryContainer)}
.esub .dvsegi.on .ms{width:17px;opacity:1;transform:scale(1)}
.esub .dvsegi:active{transform:scale(.94)}
```
DOM: 2 button, icon `check` + text "Tiêu thụ" / "Điện mặt trời".

Android: `EsubGroup.kt` — pill surfaceContainer, padding 4dp, gap 2dp, item weight 1f, 12.5sp/700, padding 10/10, on=primaryContainer/onPrimaryContainer, icon check 17dp + gap 6 (padding end 6dp). Icon đúng `Ms.check`.

MISMATCH:
- **#1 (CRITICAL):** C1 ở trên — `Icon(M3EIcons.Check)` không compile, cả cụm không render.
- **#2:** HTML `:active{transform:scale(.94)}` — Android item dùng `.clickable{}` trơn, **không có press scale .94**.
- **#3 (minor):** icon check hiện/ẩn: HTML animate `width 0→17px + opacity + scale(.4→1)`; Android `AnimatedVisibility(expandHorizontally+fadeIn+scaleIn)` — tương đương gần đúng, không 1-1.

Behavior chuyển tab: HTML click → hide `.pre` (opacity 0, translateY 10px) → 180ms → swap hidden → show `.pre` rồi gỡ, `vibrate(6)`. Android `EnergyScreen.kt` `selectTab`: `haptic()` → `paneVisible=false`, `delay(180)`, `setTab`, `paneVisible=true` + `paneFade` (tween 180, dy 10dp) — khớp. Minor: easing tween mặc định vs HTML `ease`; haptic `TextHandleMove` vs vibrate 6ms.

---

## 2. `.wcard` — "Năng lượng sử dụng" (WeekCard, EnergyConsTab.kt)

HTML:
```css
.wcard{background:var(--surfaceHighest);border:0;border-radius:32px;padding:20px}
.wcard .wtop{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:4px}
.wcard .wt{font-size:14px;font-weight:700;color:var(--onSurface)}
.wcard .wsub{font-size:12px;color:var(--onSurfaceVariant);font-weight:500;margin-top:2px}
.wcard .wv{font-size:26px;font-weight:800;font-variant-numeric:tabular-nums;letter-spacing:-.3px}
.wcard .wv small{font-size:13px;font-weight:600;color:var(--onSurfaceVariant)}
.wbars{display:flex;align-items:flex-end;gap:8px;height:120px;margin-top:10px}
.wbar{flex:1;display:flex;flex-direction:column;align-items:center;gap:6px;height:100%;justify-content:flex-end;background:none;border:0;font-family:inherit;cursor:pointer;padding:0}
.wbar i{display:block;width:100%;max-width:34px;margin:0 auto;border-radius:999px;background:var(--primaryContainer);transition:height .8s var(--motion-emphasized)}
.wbar.today i{background:var(--primary);box-shadow:0 4px 14px color-mix(in srgb,var(--primary) 40%,transparent)}
.wbar span{font-size:10px;font-weight:600;color:var(--onSurfaceVariant)}
.wbar.today span{color:var(--primary);font-weight:800}
```
JS: `wvals=[7.8,9.2,6.5,8.8,7.1,9.6,8.4]`, `wdays=['T2','T3','T4','T5','T6','T7','HN']`, `height=v/10*100%`, tick 8s `wvals[6]=min(9.9,+0.05)`.

Android: `M3ECard(shape=32dp, padding 20dp)`, wt 14sp/Bold, wsub 12sp/Medium margin-top 2dp, wv 26sp/ExtraBold tnum ls -.3sp, small 13sp/SemiBold, wbars height 120dp gap 8dp, bar max-width 34dp radius 999 primaryContainer, today=primary, label 10sp/SemiBold (today primary/ExtraBold), tick 8s +0.05 max 9.9, label today "HN" (EnergyConsTab.kt:179). Màu nền thẻ: xem #8.

MISMATCH:
- **#4:** Chiều cao cột — HTML `height:84%` của container 120px (= 100.8px với 8.4 kWh); Android `EnergyConsTab.kt:193` `.height((100 * h).dp)` = **84dp tuyệt đối**. Mọi cột thấp hơn HTML ~17%.
- **#5:** HTML `.wtop{margin-bottom:4px}` — Android không có gap giữa wtop Row và Spacer(10dp) → thiếu 4dp (10dp vs 14px).
- **#6:** HTML `.wbar` là `<button>` nhưng **không có** `:active` scale; Android `pressMorph(pressedScale=0.92f)` thêm behavior HTML không có.
- **#7 (minor):** today shadow HTML `0 4px 14px primary 40%`; Android `.shadow(4.dp, CircleShape, spotColor=primary 40%)` — blur khác.

---

## 3. `.cost` — "Chi phí điện" (CostCard)

HTML:
```css
.cost{background:var(--surfaceHighest);border:0;border-radius:32px;padding:20px}
.cost .ct{font-size:14px;font-weight:700;margin-bottom:12px}
.stat2row{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:10px}
.stat2{background:var(--surfaceContainer);border-radius:22px;padding:14px}
.stat2 .l{font-size:11.5px;font-weight:600;color:var(--onSurfaceVariant)}
.stat2 .v{font-size:19px;font-weight:800;margin-top:4px;font-variant-numeric:tabular-nums}
.stat2 .v small{font-size:12px;font-weight:600;color:var(--onSurfaceVariant)}
.pboxrow{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px}
.pbox{background:var(--surfaceContainer);border-radius:22px;padding:12px;text-align:center}
.pbox .l{font-size:10.5px;font-weight:600;color:var(--onSurfaceVariant)}
.pbox .v{font-size:15px;font-weight:800;margin-top:4px;font-variant-numeric:tabular-nums;color:var(--primary)}
```
JS tick 9s: `cGrid=18450+rand(120)`, `cHome=26880+rand(200)`, format `vi-VN`.

Android: radius 32/padding 20 ✓, ct 14/Bold + Spacer 12 ✓, stat2row 2 col gap 10 + Spacer 10 ✓, stat2 radius 22/padding 14 ✓, l 11.5/SemiBold ✓, v 19/ExtraBold tnum + top 4 ✓, small 12/SemiBold ✓, pboxrow 3 col gap 10 ✓, pbox radius 22/padding 12 center ✓, l 10.5 ✓, v 15/ExtraBold tnum primary + top 4 ✓, format `vi-VN` ✓.

MISMATCH:
- **#8 (hệ thống):** HTML mọi thẻ dùng `background:var(--surfaceHighest)` (= `#FFFFFF` light / `#35302A` dark seed cam); Android `M3ECard.kt:27` mặc định `containerColor = surfaceContainerHighest`, **không có token `surfaceHighest`** trong theme → mọi thẻ Điện (wcard, cost, pwcard, donut, dvcard, blw, flcard, syn, exp) sai màu nền.
- **#9 (behavior minor):** HTML tick 9s cập nhật 2 số VND; Android không có tick — số chỉ đổi khi HA refresh.

---

## 4. `.pwcard` — "Công suất hoạt động" (PowerCard)

HTML:
```css
.pwcard{background:var(--surfaceHighest);border:0;border-radius:32px;padding:20px}
.pwcard .pt{font-size:14px;font-weight:700;margin-bottom:4px}
.pwcard .psub{font-size:12px;color:var(--onSurfaceVariant);font-weight:500;margin-bottom:12px}
.pwr{margin-bottom:12px} .pwr:last-child{margin-bottom:0}
.pwr .pr{display:flex;justify-content:space-between;font-size:12.5px;font-weight:600;margin-bottom:6px}
.pwr .pr b{font-variant-numeric:tabular-nums;font-weight:800}
.pwr .ptrack{height:10px;border-radius:99px;background:var(--surfaceContainerHigh);overflow:hidden}
.pwr .pfill{display:block;height:100%;border-radius:99px;transition:width .8s var(--motion-emphasized)}
```
JS: rows Pin −600 (#D97706, dấu `−` U+2212) / Điện mặt trời 2700 (primary) / Lưới 300 (info) / Tiêu thụ 1800 (tertiary); width `min(100,|v|/7000*100)%`; tick 5s random.

Android: radius 32/padding 20 ✓, pt 14/Bold, psub 12/Medium, rows spacedBy 12 ✓, pr 12.5/SemiBold + Spacer 6 ✓, value 12.5/ExtraBold tnum + dấu `−` ✓, ptrack 10dp/surfaceContainerHigh ✓, pfill tween 800 emphasized ✓, màu 4 hàng đúng ✓, tick 5s jitter ✓. Dữ liệu từ HA thật (đúng quy tắc data thật).

MISMATCH:
- **#10:** HTML `.pwcard .pt{margin-bottom:4px}` — Android `EnergyConsTab.kt` (PowerCard) đặt Text pt sát Text psub, **thiếu 4dp** (chỉ có Spacer 12dp sau psub).

---

## 5. Donut "Cơ cấu tiêu thụ" (DonutCard + EnergyDonut.kt)

HTML:
```css
.ecard{background:var(--surfaceHighest);border:0;border-radius:40px;padding:20px}
.etop{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}
.elabel{font-size:12px;font-weight:500;color:var(--onSurfaceVariant)}
.evalue{font-size:30px;font-weight:800;letter-spacing:-.3px;color:var(--onSurface);font-variant-numeric:tabular-nums}
.evalue small{font-size:13px;font-weight:700}
.eicon{width:52px;height:52px;border-radius:26px;background:var(--tertiaryContainer);display:flex;align-items:center;justify-content:center}
.eicon .ms{font-size:28px;color:var(--onTertiaryContainer)}
.donutwrap{display:flex;align-items:center;gap:18px;margin-top:14px}
.donut{width:132px;height:132px;flex:none}
.dtrack{stroke:var(--surfaceHigh)}            /* stroke-width=16 */
.dseg{stroke-dashoffset:var(--off);fill:none;stroke-width:16;stroke-linecap:round}
.dcenter{font-size:19px;font-weight:800;color:var(--onSurface)}
.dli{display:flex;align-items:center;gap:10px;font-size:12px;font-weight:600;color:var(--onSurfaceVariant);opacity:0;transform:translateX(12px);transition:all .5s var(--motion-emphasized)}
.dli i{width:12px;height:12px;border-radius:6px;flex:none}
.dli b{margin-left:auto;color:var(--onSurface);font-weight:800}
```
SVG: r=54 (chu vi 339.29), 3 seg primary/tertiary/info, rotate(-90); center "8.4" (y 68) + "kWh hôm nay" (10px/600). Legend: Điều hoà 45% / Bếp 30% / Đèn & khác 25%. Entrance: `draw 1.1s` stagger .18/.36s; legend delay 0/.25/.4s.

Android: radius 40/padding 20 ✓, etop Spacer 14 ✓, elabel 12/Medium ✓, evalue 30/ExtraBold ls -.3 tnum ✓, small 13/Bold ✓, eicon 52dp/tertiaryContainer/`Ms.donut_large` 28dp ✓, donut 132dp/track surfaceHigh/stroke 16 round ✓, center 19/ExtraBold + "kWh hôm nay" 10/SemiBold ✓, legend gap 10, dot 12dp, b onSurface/800 ✓. Dữ liệu từ HA (top-3 + "Khác") — đúng quy tắc data thật.

MISMATCH:
- **#11:** Legend entrance — HTML delays **0 / .25 / .4s**; Android `staggerEnter(baseDelayMs=250, staggerMs=150)` → **250 / 400 / 550ms** (item đầu trễ 250ms so với 0).
- **#12 (minor):** Android trừ `3f` khỏi sweep mỗi slice (`EnergyDonut.kt`: `sweep - 3f`) tạo khe hở; HTML các seg liền nhau (chỉ bo round cap).

---

## 6. `.dvx` — "Thiết bị tiêu thụ" (DevicesCard)

HTML:
```css
.dvx{margin-bottom:14px}
.dvx .dvcard{background:var(--surfaceHighest);border:0;border-radius:32px;padding:20px 16px 12px;box-shadow:var(--shadow);width:100%}
.dvx .dvhead{display:flex;align-items:center;justify-content:space-between;margin-bottom:4px}
.dvx .dvtitle{font-size:17px;font-weight:700}
.dvx .dvseg{display:flex;background:var(--surfaceContainer);border-radius:999px;padding:4px;gap:2px}
.dvx .dvsegi{flex:1;display:flex;align-items:center;justify-content:center;gap:6px;border:0;background:transparent;color:var(--onSurfaceVariant);font-family:inherit;font-size:12px;font-weight:600;padding:8px 10px;border-radius:999px;cursor:pointer;white-space:nowrap;transition:background .35s var(--ease),color .3s,transform .3s var(--spring)}
.dvx .dvsegi .ms{font-size:16px;width:0;opacity:0;transform:scale(.4);overflow:hidden;transition:width .3s var(--ease),opacity .25s,transform .35s var(--spring)}
.dvx .dvsegi.on{background:var(--primaryContainer);color:var(--onPrimaryContainer)}
.dvx .dvsegi.on .ms{width:16px;opacity:1;transform:scale(1)}
.dvx .dvsegi:active{transform:scale(.94)}
.dvx .dvrow{display:flex;align-items:center;background:var(--surfaceContainer);border:0;border-radius:20px;padding:12px 14px;margin-top:8px}
.dvx .dvname{font-size:14px;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.dvx .dvsub{font-size:11px;color:var(--onSurfaceVariant);margin-top:2px}
.dvx .dvval{text-align:right;font-size:18px;font-weight:700;color:var(--primary);font-variant-numeric:tabular-nums;white-space:nowrap}
.dvx .dvunit{text-align:right;font-size:11px;color:var(--onSurfaceVariant);margin-top:2px}
.dvx .dvcost{margin-left:12px;min-width:64px;text-align:right}
.dvx .dvcostv{font-size:14px;font-weight:700;color:#16A34A;font-variant-numeric:tabular-nums;white-space:nowrap}
```
JS: sort desc + top 5; power: `w>=1000 → (w/1000).toFixed(1)+'kW'` else `round+'W'`; energy: `kwh.toFixed(1)+'kWh'` + cost `kwh*3200` VND format `.`; sub "Vừa xong"/"Hôm nay"; đổi mode: rows `.pre` → 180ms → stagger 45ms; tick 2.8s jitter ±8%.

Android (`EnergyConsTab.kt` DevicesCard/DeviceRow): card padding 20/16/12 + shadow 12dp ✓, dvhead ✓, title 17/Bold ✓, dvseg pill/padding 4/gap 2 ✓, dvsegi 12sp/SemiBold padding 8/10 ✓, on=primaryContainer ✓, check 16dp + gap 6 ✓, pressMorph .94 ✓ (= `:active .94`), dvrow radius 20/padding 12/14/margin-top 8 ✓, dvname 14/Medium ellipsis ✓, dvsub 11 + top 2 ✓, dvval 18/Bold primary tnum right ✓, dvunit 11 + top 2 right ✓, dvcost margin-start 12/min-width 64 ✓, dvcostv 14/Bold #16A34A tnum ✓, format số/kWh/VND ✓, sort top 5 ✓, tick 2.8s ✓, đổi mode fade 180ms + stagger 45ms ✓.

MISMATCH:
- **#13:** HTML `.dvx .dvsegi{flex:1}` — 2 item **rộng bằng nhau**; Android dvseg items (`EnergyConsTab.kt:594` vùng) **không có `weight(1f)`** → co theo nội dung ("Công suất" vs "Năng lượng" lệch nhau).
- **#14:** HTML `.dvhead{margin-bottom:4px}` — Android `Spacer(Modifier.height(8.dp))` (`EnergyConsTab.kt:627`) → **8dp vs 4px**.
- **#15:** icon check: HTML animate `width 0→16px`; Android `if (isSel)` hiện/tắt đột ngột, không animate width.

---

## 7. `.blw` — "Pin thiết bị yếu" (LowBatteryCard)

HTML:
```css
.blw{background:var(--surfaceHighest);border:0;border-radius:32px;padding:8px 20px}
.blw .bt2{font-size:14px;font-weight:700;padding:12px 0 4px}
.blr{display:flex;align-items:center;gap:12px;padding:11px 0;border-top:1px solid var(--surfaceContainerHigh)}
.blr:first-of-type{border-top:0}
.blr .ms{font-size:22px;color:var(--onSurfaceVariant)}
.blr .n{flex:1;font-size:13px;font-weight:600}
.blr .v{font-size:13px;font-weight:800;font-variant-numeric:tabular-nums}
.blr.low .v{color:var(--error)}
.blr .btrack{width:64px;height:6px;border-radius:99px;background:var(--surfaceContainerHigh);overflow:hidden}
.blr .btrack i{display:block;height:100%;border-radius:99px;background:var(--primary)}
.blr.low .btrack i{background:var(--error)}
```
Data: Cảm biến cửa chính/`door_front`/18 (low ≤20) / Remote phòng khách/`remote_gen`/45 / Cảm biến PIR T2/`sensors`/52 / Khoá cửa sau/`lock`/63.

Android: padding 8/20 ✓, bt2 14/Bold + padding top 12/bottom 4 ✓, rows gap 12/padding vertical 11 + divider surfaceContainerHigh ✓ (tương đương border-top), icon 22dp/onSurfaceVariant ✓, n 13/SemiBold weight 1 ✓, v 13/ExtraBold tnum ✓, low→error (≤20) ✓, btrack 64×6/surfaceContainerHigh ✓, fill primary / low→error ✓. Dữ liệu từ HA (<70%) — đúng quy tắc data thật.

MISMATCH:
- **#16:** icon "Remote…": HTML `remote_gen`; Android `lowBattIcon` map "remote" → `Ms.settings_remote` (không có `remote_gen` trong subset). `door_front`/`sensors`/`lock` đúng.

---

## 8. `.flx` — Flow card "Năng lượng" (EnergyFlowCard.kt)

HTML:
```css
.flx{margin-bottom:14px}
.flx .flcard{background:var(--surfaceHighest);border:0;border-radius:32px;padding:20px 16px 16px;box-shadow:var(--shadow);width:100%}
.flx .flhead{display:flex;justify-content:space-between;align-items:flex-start}
.flx .fltitle{font-size:20px;font-weight:700}
.flx .flsub{font-size:12px;color:var(--onSurfaceVariant);margin-top:3px}
.flx .flcap{font-size:22px;font-weight:700;text-align:right;white-space:nowrap}
.flx .flcap small{font-size:13px;font-weight:500;color:var(--onSurfaceVariant)}
.flx .fllive{display:inline-block;width:8px;height:8px;border-radius:50%;background:#22C55E;margin-right:7px;animation:flblink 1.6s ease-in-out infinite;vertical-align:2px}
@keyframes flblink{50%{opacity:.3}}
.flx .flflow{position:relative;width:100%;aspect-ratio:360/340;margin-top:6px}
.flx .fltrack{fill:none;stroke:var(--outlineVariant);stroke-width:2;opacity:.45;stroke-linecap:round;stroke-linejoin:round}
.flx .flsweep{fill:none;stroke:var(--primary);stroke-width:5;stroke-linecap:round;stroke-dasharray:70 1200;animation:flsweepm var(--dur,7s) linear infinite;filter:drop-shadow(0 0 7px color-mix(in srgb,var(--primary) 70%,transparent))}
@keyframes flsweepm{to{stroke-dashoffset:-1270}}
.flx .flsweep.rev{animation-direction:reverse}
.flx .flhub{position:absolute;left:50%;top:49.4%;transform:translate(-50%,-50%);width:64px;height:64px;border-radius:50%;background:var(--primaryContainer);display:flex;align-items:center;justify-content:center;box-shadow:var(--shadow);z-index:2}
.flx .flhub .ms{font-size:30px;color:var(--onPrimaryContainer)}
.flx .flhub::before{content:'';position:absolute;inset:0;border-radius:50%;border:2px solid var(--primary);animation:flping 2.2s ease-out infinite}
@keyframes flping{0%{transform:scale(1);opacity:.7}80%,100%{transform:scale(1.9);opacity:0}}
.flx .flnode{position:absolute;width:118px;height:132px;display:flex;flex-direction:column;background:var(--surfaceContainer);border:0;border-radius:20px;padding:10px 12px;box-shadow:var(--shadow);cursor:pointer;transition:transform .35s var(--motion-spring);z-index:2;color:var(--onSurface);text-align:left;font-family:inherit}
.flx .flnode:active{transform:scale(.93)}
.flx .fln-tl{left:6px;top:6px} .flx .fln-tr{right:6px;top:6px} .flx .fln-bl{left:6px;bottom:6px} .flx .fln-br{right:6px;bottom:6px}
.flx .flnic{width:36px;height:36px;border-radius:50%;background:var(--tint);display:flex;align-items:center;justify-content:center;margin-bottom:8px}
.flx .flnic .ms{font-size:20px;color:var(--tc)}
.flx .flnlab{font-size:10.5px;color:var(--onSurfaceVariant);white-space:nowrap}
.flx .flnval{font-size:15px;font-weight:700;margin-top:1px}
.flx .flnval small{font-size:11px;font-weight:500;color:var(--onSurfaceVariant)}
.flx .flsoc{margin-top:auto;display:block}
.flx .flsoct{display:flex;justify-content:space-between;font-size:10px;font-weight:600;color:var(--onSurfaceVariant);margin-bottom:4px}
.flx .flsocbar{display:block;height:4px;border-radius:2px;background:var(--outlineVariant);overflow:hidden}
.flx .flsocbar i{display:block;height:100%;background:#16A34A;border-radius:2px;transition:width .6s ease}
.flx .flbdir{font-size:9px;font-weight:700;color:var(--tc);background:var(--tint);border-radius:999px;padding:2px 7px;margin-left:5px;white-space:nowrap}
.flx .flsubs{margin-top:auto;display:block}
.flx .flsegbar{display:flex;height:4px;border-radius:99px;overflow:hidden;background:var(--surfaceContainerHigh);gap:2px}
.flx .flsegleg{display:flex;gap:8px;margin-bottom:5px;font-size:10px;font-weight:600;color:var(--onSurfaceVariant)}
.flx .flsegleg span{display:flex;align-items:center;gap:3px}
.flx .flsegleg i{width:6px;height:6px;border-radius:50%;flex:none}
.flx .flsegbar i{display:block;border-radius:99px;transition:width .8s cubic-bezier(.05,.7,.1,1);min-width:8px}
.flx .flfoot{margin-top:12px;padding-top:12px;border-top:1px solid var(--outlineVariant);display:flex;justify-content:space-between;font-size:12px;color:var(--onSurfaceVariant)}
.flx .flfoot b{color:var(--onSurface);font-weight:700}
```
Node: Sản xuất `solar_power` (tint rgba(245,158,11,.16)/#D97706) + PV1/PV2 segbar (primary 56% / tertiary 44%) · Lưới điện `electric_meter` (rgba(47,110,163,.16)/#2F6EA3) · Tiêu thụ `home` (primary 14% mix/primary) + CB1/CB2/CB3 (primary 39% / tertiary 33% / secondary 28%) · Pin `battery_charging_full`↔`battery_full` (rgba(34,197,94,.16)/#16A34A) + badge "Đang sạc"/"Đang xả" + SOC bar. Hub icon `bolt`.
PATHS: prod `M124 50H146Q162 50 162 66V134`, grid `M198 134V66Q198 50 214 50H236`, cons `M162 202V274Q162 290 146 290H124`, batt `M198 202V274Q198 290 214 290H236`.
Tốc độ: `spd=clamp(4,14,18/max(.15,v))` giây; chỉ đổi `--dur` khi lệch >12%. Pin toggle: đổi text/icon/chiều sweep. Tick 2.8s: pv/cb jitter, seg widths, SOC +0.4/−0.3 (5–100).
Footer: "Hôm nay sản xuất **12.5 kWh**" | "Tự dùng **81%**".

Android: card padding 20/16/16 + shadow 12dp ✓, flhead Top ✓, title 20/Bold ✓, flsub 12 ✓, flcap 22/Bold right + small 13/Medium ✓, live dot 8dp #22C55E + gap 7 + blink(1600) ✓, flflow aspect 360/340 + Spacer 6 ✓, nodes 118×132 offset 6px ✓, radius 20/padding 10/12 ✓, bg surfaceContainer ✓, press .93 ✓, flnic 36dp + Spacer 8 ✓, ms 20dp ✓, tints 4 node đúng ✓, icons đúng 5/5 ✓, flnlab 10.5 ✓, flnval 15/Bold + top 1 ✓, small 11/Medium ✓, flbdir 9/Bold/padding 2/7/gap 5 ✓, flsubs margin-top auto (Spacer weight 1) ✓, segleg gap 8 + Spacer 5, 10/SemiBold, dot 6dp ✓, segbar 4dp/surfaceContainerHigh/gap 2 ✓, flsoc auto ✓, flsoct 10/SemiBold + Spacer 4 ✓, flsocbar 4dp/outlineVariant/fill #16A34A ✓, hub 64dp/primaryContainer/shadow, bolt ~30dp ✓, ping 2.2s (scale 1→1.9, alpha .7→0, 80/100 keyframes) ✓, track outlineVariant .45/2dp/round ✓, sweep dash 70/1200, width 5, primary, hướng đảo khi xả ✓, tốc độ `sweepMs` đúng công thức ✓, 4 PATHS khớp từng tọa độ ✓, pin toggle đổi badge/icon/chiều ✓, footer divider + 12sp/bold ✓.

MISMATCH:
- **#17:** HTML `.flx .flsub{margin-top:3px}` — Android `EnergyFlowCard.kt:117` ("Dòng chảy thời gian thực") **không có margin-top 3dp**.
- **#18:** HTML `.flsegbar i{transition:width .8s cubic-bezier(.05,.7,.1,1)}` — Android `SegBar` **không animate width** (nhảy cục khi recompose).
- **#19:** HTML `.flsocbar i{transition:width .6s ease}` — Android SOC bar **không animate width**.
- **#20:** Glow sweep: HTML `drop-shadow(0 0 7px primary 70%)`; Android vẽ thêm underlay 9dp primary 25% — khác implementation.
- **#21:** HTML chỉ set lại `--dur` khi tốc độ lệch >12% (tránh restart animation); Android đổi `tween(sweepMs)` → restart vòng sweep mỗi khi công suất đổi.
- **#22 (data note):** HTML caption `flvCap`=22.0 kWh tách biệt footer "Hôm nay sản xuất 12.5 kWh"; Android dùng `flow.todayKwh` (=pvToday) cho **cả hai**.
- **#23 (minor):** blink HTML `50%{opacity:.3}`; Android `blink()` 1→0.25 Reverse.

---

## 9. `.syn` — "Tải tiêu thụ" + "Pin S6" (SunsynkCard, EnergySolarTab.kt)

HTML:
```css
.syn{background:var(--surfaceHighest);border:0;border-radius:32px;padding:20px}
.syn .yt{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:12px}
.syn .yt .t{font-size:14px;font-weight:700}
.syn .yt .v{font-size:22px;font-weight:800;font-variant-numeric:tabular-nums}
.syn .yt .v small{font-size:12px;color:var(--onSurfaceVariant);font-weight:600}
.tier{margin-bottom:10px}
.tier .tr{display:flex;justify-content:space-between;font-size:12px;font-weight:600;color:var(--onSurfaceVariant);margin-bottom:5px}
.tier .tr b{color:var(--onSurface);font-variant-numeric:tabular-nums}
.tier .tt{height:8px;border-radius:99px;background:var(--surfaceContainerHigh);overflow:hidden}
.tier .tt i{display:block;height:100%;border-radius:99px;background:var(--tertiary)}
.syndiv{height:1px;background:var(--surfaceContainerHigh);margin:14px 0}
.syn .ph{display:flex;justify-content:space-between;align-items:center;margin-bottom:8px}
.syn .ph .t{font-size:14px;font-weight:700}
.syn .ph .s{font-size:12px;font-weight:700;color:var(--success)}
.socbar2{height:8px;border-radius:99px;background:var(--surfaceContainerHigh);overflow:hidden;margin-bottom:14px}
.socbar2 i{display:block;height:100%;border-radius:99px;background:#16A34A;transition:width .8s}
.mini4{display:grid;grid-template-columns:1fr 1fr;gap:10px}
.mini{background:var(--surfaceContainer);border-radius:20px;padding:12px 14px}
.mini .l{font-size:10.5px;font-weight:600;color:var(--onSurfaceVariant)}
.mini .v{font-size:16px;font-weight:800;margin-top:3px;font-variant-numeric:tabular-nums}
.mini .v small{font-size:11px;color:var(--onSurfaceVariant);font-weight:600}
```
Data: Tầng 1/0.8/34% · T2/0.9/38% · T3/0.7/30% (width = v/tổng 2.4); minis: Công suất 0.6 `<small>kW</small>` / Dòng · Áp 12 `<small>A</small>` · 52 `<small>V</small>` / Sạc giới hạn 40 `<small>A</small>` / Xả giới hạn 60 `<small>A</small>`.

Android: radius 32/padding 20 ✓, yt Spacer 12 ✓, t 14/Bold ✓, v 22/ExtraBold tnum + small 12/SemiBold ✓, tier Spacer 10 ✓, tr 12/SemiBold + Spacer 5 ✓, b onSurface/Bold tnum ✓, tt 8dp/surfaceContainerHigh ✓, fill tertiary ✓, syndiv Spacer 14 + divider 1dp + Spacer 14 ✓, ph 14/Bold + "SOC x%" 12/Bold success ✓ + Spacer 8 ✓, socbar2 8dp + fill #16A34A ✓, mini4 2 col gap 10 ✓, mini radius 20/padding 12/14 ✓, l 10.5 ✓, v 16/ExtraBold tnum + top 3 ✓.

MISMATCH:
- **#24:** Chuẩn hoá thanh tier — HTML width = **v/tổng** (0.8/2.4=34%, 0.9→38%, 0.7→30%); Android `EnergySolarTab.kt:163,173` dùng **v/maxTier** → 89% / 100% / 78%. Thanh dài gấp ~2.6 lần HTML.
- **#25:** Đơn vị trong mini — HTML bọc `<small>` (11px/600 onSurfaceVariant: "kW", "A", "V"); Android `MiniBox` nối chuỗi thuần 16sp/800 ("12 A · 52 V", "0.6 kW", "40 A", "60 A").
- **#26:** HTML `.socbar2 i{transition:width .8s}` — Android không animate width SOC.
- **#27 (minor):** HTML `.yt{align-items:baseline}`; Android `Alignment.Bottom`.

---

## 10. `.exp` — "Sạc pin" / "Xả pin" (Expander + ControlRow + M3ESwitch + Stepper)

HTML:
```css
.exp{background:var(--surfaceHighest);border:0;border-radius:32px;overflow:hidden}
.exph{width:100%;display:flex;align-items:center;gap:12px;background:none;border:0;font-family:inherit;padding:18px 20px;cursor:pointer;color:inherit;text-align:left}
.exph .eic{width:44px;height:44px;border-radius:22px;background:var(--primaryContainer);color:var(--onPrimaryContainer);display:flex;align-items:center;justify-content:center;flex:none}
.exph .eic .ms{font-size:24px}
.exph .et{flex:1}
.exph .et .t{font-size:14px;font-weight:700}
.exph .et .s{font-size:12px;color:var(--onSurfaceVariant);font-weight:500;margin-top:2px}
.exph .chev{transition:transform .4s var(--spring);color:var(--onSurfaceVariant)}
.exph .chev .ms{font-size:26px}
.exp.open .exph .chev{transform:rotate(180deg)}
.expbody{max-height:0;overflow:hidden;transition:max-height .5s var(--motion-emphasized)}
.exp.open .expbody{max-height:640px}
.expbodyin{padding:2px 20px 16px;display:flex;flex-direction:column}
.ctl{display:flex;align-items:center;gap:12px;padding:11px 0;border-top:1px solid var(--surfaceContainerHigh)}
.ctl .cl{flex:1;font-size:13.5px;font-weight:600}
.ctl .cs{font-size:12px;color:var(--onSurfaceVariant);font-weight:500}
.step2{display:flex;align-items:center;gap:10px}
.step2 button{width:32px;height:32px;border-radius:50%;border:0;background:var(--surfaceContainer);color:var(--onSurface);font-size:16px;font-weight:700;cursor:pointer;font-family:inherit;display:flex;align-items:center;justify-content:center;transition:transform .25s var(--spring)}
.step2 button:active{transform:scale(.85)}
.step2 b{font-size:14px;font-weight:800;min-width:56px;text-align:center;font-variant-numeric:tabular-nums}
.tgl{width:52px;height:32px;border-radius:16px;background:var(--surfaceHighest);position:relative;cursor:pointer;flex:none;box-sizing:border-box;border:2px solid var(--outline);transition:background .3s var(--ease),border-color .3s var(--ease)}
.tgl .knob{position:absolute;top:6px;left:6px;width:16px;height:16px;border-radius:50%;background:var(--outline);display:flex;align-items:center;justify-content:center;overflow:hidden;transition:left .45s var(--spring),top .3s,width .3s,height .3s,background .3s,transform .3s}
.tgl .knob .ms{font-size:13px;color:var(--onPrimaryContainer);opacity:0;transform:scale(.5);transition:opacity .2s,transform .3s var(--spring)}
.tgl.on{background:var(--primary);border-color:var(--primary)}
.tgl.on .knob{left:24px;top:4px;width:20px;height:20px;background:var(--onPrimary)}
.tgl.on .knob .ms{opacity:1;transform:scale(1)}
.tgl:active .knob{transform:scaleX(1.15)}
```
Header icons: `battery_charging_full` / `battery_full`, chevron `expand_more`.

Android: radius 32 + clip ✓, exph padding 18/20 + gap 12 ✓, eic 44dp/primaryContainer/ms 24dp ✓, icons đúng ✓, t 14/Bold ✓, s 12/Medium + top 2 ✓, chevron 26dp/onSurfaceVariant/rotate 180 tween 400 spring ✓, body expand/shrink 500 emphasized ✓, bodyin padding 2/20/16 ✓, ctl padding vertical 11 + divider ✓, cl 13.5/SemiBold weight 1 ✓, cs/time 12/Medium ✓, step2 gap 10 ✓, nút 32dp/surfaceContainer/16sp/Bold/press .85 ✓, value 14/ExtraBold/min-width 56/center tnum ✓, switch 52×32/border 2dp outline/knob 16→20/left 6→24/top 6→4/check 13dp/press scaleX 1.15 ✓. Toggle exph + vibrate — Android `clickable{onToggle()}` (không vibrate riêng; haptic chung nếu có).

MISMATCH:
- **#28 (CRITICAL):** C2 — `M3ESwitch.kt:113` `Icon(imageVector=M3EIcons.Check)` không compile → mọi switch Sạc/Xả gãy.
- **#29:** HTML `.exp{background:var(--surfaceHighest)}` — Android `EnergySolarTab.kt:300` dùng `surfaceContainerHighest` (cùng gốc #8).
- **#30:** HTML `.ctl{gap:12px}` — Android `ControlRow` (`EnergySolarTab.kt:365-371`) **không có gap ngang** giữa label (weight 1f) và control (switch/stepper/time) → 0 vs 12px.
- **#31:** HTML `.tgl{background:var(--surfaceHighest)}` (trắng khi tắt) — Android `M3ESwitch.kt:85` dùng `surfaceContainerHighest` (cùng gốc #8).
- (minor) knob top/size transition: HTML dùng default ease (.3s), Android dùng spring.
- (data note) subtitle: HTML cố định "9 điều khiển"/"3 điều khiển"; Android `${size} điều khiển` theo entity HA thật. Hàng control: HTML 9 hàng cố định (Sạc AC=tgl on, Sạc DC=tgl on, SOC kết thúc=step 100%…); Android dựng từ entity Solis thật, tên/loại khác (data thật — đúng quy tắc).

---

## Thứ tự & nhịp chung

- Thứ tự thẻ cons: wcard → cost → pwcard → donut → dvx → blw ✓ (khớp JS move). Solar: flow → syn → expChg → expDis ✓.
- `.etab>*` margin 14px: Android `spacedBy(14.dp)` trong ConsTab/SolarTab ✓. `.esub` margin-bottom 14: LazyColumn `spacedBy(14)` ✓.
- Rise: HTML `.rise{animation:rise .7s var(--ease)}` (`--ease` = emphasized) translateY 22px; Android `riseOnce` 700ms emphasized dy 22dp, chạy 1 lần ✓.
- Header "Điện" 26/Bold/ls -.3 + sub 13/top 3 ✓. Page padding: HTML `.page{padding:4px 18px 170px}` — Android thiếu top 4dp (minor).

---

## TỔNG: 31 mismatches + 2 lỗi compile critical

| # | Mức | Vị trí |
|---|-----|--------|
| C1/#1 | CRITICAL compile | EsubGroup.kt:73 `Icon(M3EIcons.Check)` — String vs ImageVector |
| C2/#28 | CRITICAL compile | M3ESwitch.kt:113 `Icon(imageVector=M3EIcons.Check)` |
| #8,#29,#31 | Sai màu hệ thống | Thiếu token `surfaceHighest`; M3ECard/M3ESwitch/Expander dùng `surfaceContainerHighest` |
| #4 | Kích thước | WeekBar height `(100*h).dp` thay vì % của 120dp |
| #24 | Kích thước | Tier bar chuẩn hoá theo max thay vì tổng |
| #13 | Layout | dvseg items thiếu `flex:1` (weight) |
| #30 | Layout | ControlRow thiếu gap 12px |
| #5,#10,#12,#14,#17 | Spacing | thiếu/thừa 3–4dp (wtop, pwcard pt, dvhead, flsub) |
| #18,#19,#26 | Behavior | segbar/SOC bar không animate width |
| #2,#15 | Behavior | esub thiếu press .94; dvseg icon không animate width; blw icon `remote_gen`→`settings_remote` |
| #6,#7,#9,#11,#20,#21,#23,#27 | Minor | press thừa, shadow, easing, timing legend/donut, glow, restart sweep, blink, baseline |
| #9,#22 + data notes | Data | cost tick 9s không có; caption/footer cùng todayKwh; donut/devices/blw/controls từ HA thật (đúng quy tắc) |

Khuyến nghị thứ tự sửa: C1+C2 (compile) → #8 (token surfaceHighest) → #4 → #24 → #13 → #30 → spacing (#5,#10,#12,#14,#17) → behavior (#18,#19,#26,#2,#15) → minor.
