# AUDIT: Tab NHÀ — HTML rev12 vs Android (nhánh `feat/m3e-dashboard`)

- **HTML spec (duy nhất):** `/home/hatch/workspace/your_files/hume-m3e-v4-dashboard.html` (rev12), section `#page-home`
- **Android:** repo `~/workspace/Hume-Android`, nhánh `feat/m3e-dashboard`, HEAD `24cd7bd` (2026-09-28), `git status` sạch
- **Nguyên tắc:** copy 1-1, `px` HTML = `dp/sp` Android 1:1; icon đối chiếu ligature trong DOM `<span class="ms">`, không đoán từ ảnh
- **Phạm vi:** AUDIT ONLY — không sửa code. Đây là **static audit** (đọc code, không compile/build được trong sandbox); mọi tuyên bố "khớp" là khớp static, chưa đối chiếu build thật trên S26 Ultra.

> **Hiệu chỉnh quan trọng so với phát hiện sơ bộ:** HTML **CÓ** nhịp dọc 14px chuẩn — `.tabpage>*{margin-bottom:14px}` (dòng 966, cùng specificity với `.pills` dòng 195 nhưng viết sau nên thắng `margin-bottom:6px`), `.tabpage>.sec{margin-bottom:12px}` (dòng 968). Margins collapse giữa các block siblings → khoảng cách thực: header→pills **14px**, pills→sec **20px** (max 14/20), sec→card **12px**, card→card **14px**.

---

## A. Nhịp dọc trang (`HomeScreen.kt:81`, LazyColumn `spacedBy(14.dp)`)

| HTML | Android |
|---|---|
| `.tabpage>*{margin-bottom:14px}`; `.tabpage>.sec{margin-bottom:12px}`; collapse → pills→sec 20px, sec→card 12px, còn lại 14px | `LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp))` + `SectionTitle` tự padding top 20 / bottom 10 |

- **M1** — pills → SectionTitle: Android = 14 (spacedBy) + 20 (title padding) = **34dp** vs HTML **20px** (thừa 14dp, do padding của title không collapse như margin CSS).
- **M2** — SectionTitle → card: Android = 10 + 14 = **24dp** vs HTML **12px** (thừa 12dp).
- Card→card 14dp ✓ đúng nhịp HTML.

## B. Header (`HomeCards.kt:71` `HomeHeader`)

| HTML | Android |
|---|---|
| `.hhome{display:flex;align-items:center;gap:10px;padding:8px 2px 2px}` (gap 12 dòng 376 bị override → 10px dòng 396) | Row padding top 8 / bottom 2 / start-end 2 ✓; Spacer 12dp (avatar→text), weight 1f (text→search, **không gap**), Spacer 10dp (search→bell) |
| `.hava{55px;bg:var(--surfaceHighest)}` `.hava .ms{28px}` | 55dp ✓, icon 28dp ✓, bg `surfaceContainerHighest` |
| `.pdot{16px;border:3px solid var(--surface);box-sizing:content-box}` → tổng ngoài **22px**, viền vẽ RA NGOÀI | `.size(16.dp).border(3.dp)` — viền vẽ TRONG 16dp |
| `.hhi{22px/700/ls -.2px}`, `.hsub{13px/500/mt 2px}` | 22sp/Bold/-0.2sp ✓, 13sp/Medium/top 2 ✓ |
| `.hhome .hchipbtn,.hhome .hbell{46px}`, `.ms{22px}`; `:active{scale(.88)}` | 46dp ✓, icon 22dp ✓, `pressMorph(0.88f)` ✓ |
| `.hbell .bdg{top:-2;right:-2;min-width:18px;height:18px;padding:0 5px}` | offset x=2/y=-2, height 18, min-width 18, hpad 5 ✓ |

- **M3** — gap avatar→text: **12dp** vs HTML **10px**.
- **M4** — gap text→search: **0** (weight 1f đứng liền) vs HTML **10px** (gap đồng đều).
- **M5** — presence dot: viền vẽ trong 16dp → dot xanh còn **10dp** vs HTML dot xanh **16px** + viền 3px ngoài (tổng 22px). Nhỏ hơn spec rõ rệt.
- **M6** — avatar bg dùng `surfaceContainerHighest` thay vì `--surfaceHighest` (giá trị trùng ở seed cam/green đã kiểm tra: `#FFFFFF`/`#35302A`, nhưng sai token — có thể lệch ở seed khác).

## C. Pills an ninh (`HomePills.kt:66` `PillsRow`)

Collapsed pill (HTML dòng 195–204): `.pill{padding:14px;gap:11px;border-radius:28px;background:var(--surfaceHighest)}`, `.pic{44px}`, `.pl{14px/700}`, `.ps{12px/500/mt 1px}`, `:active{scale(.93);border-radius:18px}`.

| HTML | Android |
|---|---|
| padding 14, gap 11, pic 44, pl 14/700, ps 12/500 mt 1, pressed radius 18 | `PillShell` (dòng 180): padding 14 ✓, gap 11 ✓, `PillIcon` 44 ✓, `PillTexts` 14/Bold ✓ 12/Medium top 1 ✓, pressed radius 18 ✓ scale .93 ✓ |
| toggle expand = thêm class `.secon` trên **cùng DOM** `#pills`; `.secmodes` hiện giữa secpill và bulbPill; chỉ secmodes animate `smIn` | `AnimatedContent` swap **toàn bộ** collapsed Row ↔ expanded Row (fade + expandHorizontally) |
| `.pills.secon{overflow-x:auto}` — hàng scroll **ngang**: secpill min-width 150px, smode 92px, bulbPill min-width 128px | expanded: Row `horizontalScroll` ✓, SecPill width 150 ✓, BulbPill width 128 ✓ — layout khớp |
| `.smode{flex:0 0 92px;column;radius 26px;padding:14px 10px;gap:12px;icon 24px;label 12px/700;selected=primaryContainer}` | `SecurityModes` (dòng 267): width 92 ✓ radius 26 ✓ padding 14/10 ✓ icon 24 ✓ label 12/Bold ✓ selected primaryContainer ✓ |
| `.smode` vào: `smIn .45s var(--spring)`, stagger `.06/.12/.18s` (từ opacity 0, translateX 18px, scale .9) | `AnimatedVisibility` fadeIn + slideInHorizontally(18dp) + scaleIn(.9), tween 450 spring, stagger 60ms ✓ gần đúng |
| chọn mode → tự đóng expanded sau **1000ms** | không tự đóng (`expanded` giữ nguyên sau `onArm`/`onDisarm`) |
| CFG đổi sec pill theo mode (dòng 1683–1686): home `shield`+successContainer; away `flight_takeoff`+tertiaryContainer; night `bedtime`+secondaryContainer; off `power_settings_new`+surfaceContainer; subtext tương ứng | `SecPill` (dòng 133) luôn icon `Shield`; chỉ phân biệt armed (successContainer) / disarmed (surfaceContainer) + `alarm.label` |
| `#bulbPill` **không có** click handler trong JS | `BulbPill` onClick → mở `LightsSheet` (behavior thêm, không có trong HTML) |

- **M7** — Animation/hierarchy expand: HTML giữ nguyên DOM, chỉ `.secmodes` animate entrance; Android `AnimatedContent` crossfade cả hàng (layout nhảy, sai behavior).
- **M8** — Không tự đóng expanded row sau 1000ms như HTML.
- **M9** — `SecPill` không swap icon/subtext/container theo từng mode (home/away/night/off) như CFG HTML.
- **M10** — `BulbPill` bấm được trong Android; HTML không gắn handler (no-op).

## D. SectionTitle (`HomeScreen.kt:263`)

| HTML | Android |
|---|---|
| `.sec{margin:20px 4px 10px}` + `.tabpage>.sec{margin-bottom:12px}` → **20/4/12**; `h3{16px/700}` | padding top 20 ✓ start/end 4 ✓, **bottom 10**; 16sp/Bold ✓ |

- **M11** — bottom padding **10dp** vs HTML **12px** (lệch 2dp; xem thêm M2 về double-count với spacedBy).

## E. Suggest card (`HomeCards.kt:191`)

| HTML (dòng 326–336, DOM 1009–1013, JS 1487–1491) | Android |
|---|---|
| `.suggest{bg:tertiaryContainer;radius 28px;padding:16px 18px;gap:14px}` | M3ECard tertiaryContainer ✓ 28 ✓ padding 16/18 ✓, Row **không** spacedBy (dùng Spacer riêng — cần check = 14) |
| icon là `<span class="ms">auto_awesome</span>` **trần**, `font-size:30px`, màu onTertiaryContainer — **không** có vòng tròn nền | vòng tròn 48dp bg `tertiary` + icon 24dp |
| `.sgt{14px/700}`, `.sgs{12px/500;opacity:.75;mt 2px}` | 14sp/Bold ✓, 12sp/Medium alpha .75 top 2 ✓ |
| `.sgbtn{background:var(--onTertiaryContainer);color:var(--tertiaryContainer);padding:12px 20px;border-radius:20px}` — màu **đảo** | bg `primary`, text `onPrimary`, padding 10/18 |
| click → text `"Đã bật"`, `disabled` (opacity .6) | → `"Đã xong"` |
| card **luôn** hiển thị (static) | `if (tip == null) return` — card biến mất |
| chỉ 1 tip duy nhất trong HTML | thêm tip pin/cửa không có trong HTML |

- **M12** — Icon: HTML icon trần 30px; Android vòng tròn 48dp + icon 24dp (sai cấu trúc & size).
- **M13** — gap icon→text: HTML **14px** (kiểm tra Spacer trong code Android).
- **M14** — nút Bật: HTML nền onTertiaryContainer/chữ tertiaryContainer (đảo); Android primary/onPrimary (sai cả 2 màu).
- **M15** — padding nút: HTML **12/20** vs Android **10/18**.
- **M16** — sau click: HTML `"Đã bật"` vs Android `"Đã xong"`.
- **M17** — HTML card không bao giờ ẩn; Android ẩn khi không có tip.
- **M18** — tip pin/cửa là nội dung thêm, không có trong HTML.

## F. Solar week chart (`HomeCards.kt:283` `SolarWeekCard`)

| HTML (dòng 437–451) | Android |
|---|---|
| `.solcard{bg:var(--surfaceHighest);radius 32px;padding:20px}` | M3ECard mặc định `surfaceContainerHighest` (sai token, xem M6); shape 32 ✓; contentPadding **vertical 18** |
| `.stop{margin-bottom:6px}`; `.st{14px/700}`; `.ssub{12px/500/mt 2px}` | Row không có bottom margin; 14sp/Bold ✓; 12sp/Medium top 2 ✓ |
| `.sval{26px/800/ls -.3px}` + `small{13px/600}` | **28sp/Bold**, small **14sp** |
| `.solsvg{width:100%;margin-top:8px}` — SVG `viewBox="0 0 320 150"` **scale theo chiều rộng thực** | container height cố định 150dp, tọa độ x cố định 20..300dp — không scale theo width |
| bar width `Math.min(34, step*.5)` = **25px** (step=50) | 24dp |
| tooltip `.soltt{bg:surfaceContainerHigh;color:onSurface;11px/700;padding 6/10;radius 12px;shadow 0 6px 18px}`; hiện bằng **hover** (`pointerenter/leave`) | bg **primary**/text **onPrimary**, không shadow; toggle bằng **tap** |
| `.soldays span{10.5px/600}`; `.today{primary/800}`; `.soldays{mt 2px}` | 10sp/Medium; today primary/**Bold**; top padding **4dp** |
| bars có `transition:height .8s var(--spring)` khi redraw | không có grow animation |

- **M19** — card dùng `surfaceContainerHighest` thay `--surfaceHighest` (sai token).
- **M20** — padding vertical **18dp** vs HTML **20px**.
- **M21** — value: **28sp/700** vs HTML **26px/800, ls -.3px**; đơn vị **14sp** vs **13px/600**.
- **M22** — thiếu `margin-bottom:6px` của `.stop`.
- **M23** — thiếu `margin-top:8px` của `.solsvg`.
- **M24** — tooltip sai màu (primary/onPrimary vs surfaceContainerHigh/onSurface), thiếu shadow.
- **M25** — nhãn ngày: 10sp/Medium vs 10.5px/600; today Bold vs 800.
- **M26** — `.soldays` margin-top: 4dp vs 2px.
- **M27** — geometry: HTML scale viewBox theo width thực; Android tọa độ cứng 20..300dp trên height 150dp cố định → vị trí/chiều cao bars lệch khi width ≠ 320dp.
- **M28** — bar width 24dp vs 25px (nhỏ).
- **M29** — tooltip: HTML hover, Android tap (behavior khác).
- **M30** — thiếu grow animation `.8s spring` khi redraw.

## G. Solar live card (`HomeCards.kt:445` `SolarLiveCard`) — KHỚP

`.solar{tertiaryContainer;radius 32px;padding:16px 18px;gap:14px}` ↔ M3ECard tertiaryContainer ✓ 32 ✓ Row padding 18/16 ✓ Spacer 14 ✓. `.sicon{48px;bg:rgba(255,255,255,.35);.ms 26px}` ↔ Box 48dp white 35% ✓ icon 26dp ✓ tint onTertiaryContainer ✓. `.sgt2{14/700}` `.sgs2{12/500/.75/mt 2}` ✓. Sparkline 90×34, path SVG copy 1-1 ✓.

## H. Battery card (`BatteryCard.kt:56`)

| HTML (dòng 453–478, DOM 1033–1045) | Android |
|---|---|
| `.batcard{bg:var(--surfaceHighest);radius 32px;padding:20px}` | M3ECard `surfaceContainerHighest` (sai token); 32 ✓; 20 ✓ |
| `.btop{mb 12px}`; `.bt{14/700}`; `.bstat{11/800/ls .6px;padding 6/12;radius 999}` | Spacer 12 ✓; 14sp/Bold ✓; 11sp/800/0.6sp, padding 12/6 ✓ |
| `.bmid{mb 10px}`; `.bsoc{34px/800/ls -.5px}` + small 14/600; `.bpw .v{18/800}` `.l{11/600/mt 2}` | Spacer 10 ✓; 34sp/800/-.5sp ✓; 18sp/800 ✓ 11sp/600 ✓ |
| `.bbar{height:14px;margin:2px 0}`: segR flat 10px, segU wavy 14px (svg 810 wide, slide -15px/1s), track 10px secondaryContainer, stop dot 4px | `AnimatedWavyBar` (dòng 225): anatomy flat 10 / khe 4 / wavy 14 / khe 4 / track 10 / stop 4 ✓; wave slide 15px/1s linear ✓; segment animate .8s emphasized ✓ |
| `.batleg{mt 8px;11.5px/600}` dot 8px + gap 6; `b{onSurface/800/ml 4}` | top 8 ✓ 11.5sp/600 ✓ dot 8 + gap 6 ✓ value 800 start 4 ✓ |
| `.batfoot{display:flex;justify-content:space-between;mt 12px;12px/500}`: `<span>Còn khoảng <b>4h12</b></span><span>Kết thúc lúc <b>18:40</b></span>` | **một** `Text(battery.timeText)` duy nhất, không có cột "Kết thúc lúc", không bold riêng value |
| JS tick mỗi **2800ms**: charging +0.4 / discharging −0.3, tính lại reserve/usage/time/end | không có local tick |

- **M31** — card bg sai token (surfaceContainerHighest vs --surfaceHighest).
- **M32** — footer: thiếu cột **"Kết thúc lúc <b>18:40</b>"** và không có bold riêng cho value; HTML là 2 span space-between.
- **M33** — không có tick SOC 2800ms (charging +0.4 / discharging −0.3 + recompute).
- **M34** — legend "Sử dụng": Canvas 24×12dp nhưng path vẽ tới x=**26dp** → bị clip; HTML svg `viewBox="0 0 28 14"` rộng 28, CSS width 24 (scale toàn vẹn).
- **M35** — khoảng cách trên thanh bar: Android Spacer 10dp vs HTML `.bmid{mb:10px}` + `.bbar{mt:2px}` = 12px (nhỏ).

## I. Grid phòng (`HomeRooms.kt:65` `RoomGrid`, :92 `RoomCard`)

| HTML (dòng 401–410, JS 1366–1384) | Android |
|---|---|
| `.roomsG{grid 1fr 1fr;gap:10px}` | `Arrangement.spacedBy(12.dp)` cả 2 chiều |
| `.roomc{bg:var(--surfaceHighest);radius 28px;padding:16px 14px}`; `.lit{bg:primaryContainer}` — `.rcn/.rcs` **giữ** onSurface/onSurfaceVariant | Box padding 14/16 ✓ radius 28 ✓; khi lit đổi text → **onPrimaryContainer** |
| `.rcic{48px;bg transparent;color:onSurfaceVariant}`; `.on{bg:primary;color:onPrimary}`; `:active{scale(.85)}` | 48dp ✓ transparent/primary ✓ tint ✓ pressMorph(0.85) ✓ |
| `.rcn{13.5px/700;mt 10px}`; `.rcs{11.5px/500/mt 3px}`: `temp · N thiết bị · M bật` | 13.5sp/Bold, Spacer 10 ✓; 11.5sp/Medium top 3 ✓; format giống ✓ |
| `.rdot2{position:absolute;top:14px;right:14px;9px;bg:error;shadow 0 0 6px}` | Box 9dp align TopEnd — **không** offset 14dp |
| room icons: bed, child_care, **temple_buddhist** (Thờ), weekend, bathtub, soup_kitchen, local_laundry_service, **sensors** (Hành lang) | `M3EIcons.room()`: bed ✓ child_care ✓ **auto_awesome** ✗ **stairs** ✗ |

- **M36** — grid gap **12dp** vs HTML **10px** (cả 2 chiều).
- **M37** — khi đèn bật: Android đổi rcn/rcs thành onPrimaryContainer; HTML chỉ đổi bg card, text giữ nguyên.
- **M38** — Phòng Thờ: `auto_awesome` vs HTML `temple_buddhist`.
- **M39** — Hành Lang: `stairs` vs HTML `sensors`.
- **M40** — rdot2 dính góc card (thiếu offset top/right 14px).
- **M41** — card bg `surfaceContainerHighest` vs `--surfaceHighest` (sai token).

## J. Room sheet (`HomeRooms.kt:193` `RoomSheet`)

| HTML (`.sheet`: bg surfaceContainer, radius 44 top, padding **12/22/34**, max-height 78%; `.grab{44×5;bg:outline;margin:4px auto 16px}`) | Android `ModalBottomSheet`: shape 44 top ✓, container surfaceContainer ✓, content padding **horizontal 20**, không set max-height; `GrabHandle` (dòng 177) 44×5/outline/top 4/bottom 16 ✓ |
| `<h2 id="shTitle">` trần | thêm icon phòng 30dp + Spacer 10dp trước title |
| `.shsub{12px/500}`; `.shenv{display:flex;gap:10px;margin:14px 0 6px}`; `.tile` temp icon **`device_thermostat`** 24px | sub ✓; Row gap 10 nhưng margin 14/6 bị nuốt bởi LazyColumn spacedBy global; temp icon `Ms.thermostat` |
| climate: `.ac-top` + `.tstep` (clamp **16..30**); `.rmm` fixed 4: **Lạnh, Khô, Quạt, Tự động** | clamp **16..31**; modes động từ HA, fallback "Làm lạnh, Sưởi, Tự động, Quạt" |
| `.dev{...padding:14px 16px;margin-bottom:10px;radius 28px}` — **không** clickable; chỉ `.tgl` bấm được | LazyColumn `spacedBy(12.dp)`; `DeviceRow` dùng `pressMorphCard(..., onClick=null)` — vẫn gắn `.clickable` + morph radius 28→18 khi nhấn |
| contact chip `.cchip{11px/800/ls .5px;padding 7/14;radius 999}` | ✓ khớp |
| scrim `rgba(0,0,0,.38)` | ModalBottomSheet scrim mặc định (theme scrim, không override) |

- **M42** — sheet: horizontal padding **20dp** vs 22px; không `max-height:78%`; ModalBottomSheet có insets/drag behavior mặc định khác custom absolute sheet HTML.
- **M43** — title thêm icon 30dp + spacer 10dp; HTML chỉ `<h2>` trần.
- **M44** — spacing nội bộ dùng một `spacedBy(12.dp)` global thay cho margin riêng: `.shenv{margin:14px 0 6px}`, `.dev{margin-bottom:10px}`.
- **M45** — env temp icon: `thermostat` vs HTML `device_thermostat`.
- **M46** — climate clamp **16..31** vs HTML **16..30**.
- **M47** — climate modes: labels/order động ("Làm lạnh, Sưởi, Tự động, Quạt") vs HTML fixed **Lạnh/Khô/Quạt/Tự động**.
- **M48** — thêm heading "Thiết bị" không có trong HTML.
- **M49** — device rows gap **12dp** vs HTML `margin-bottom:10px`.
- **M50** — `DeviceRow` vẫn clickable + morph radius khi nhấn; HTML row không clickable, không morph.
- **M51** — scrim: M3 default vs `rgba(0,0,0,.38)`.
- **M52** — `DeviceConfig` chỉ có toggle/climate, **không có contact sensors** (HTML rooms có `door_front` closed/open) — data gap.

## K. Device icons (`M3EIcons.device()`, `MsIcon.kt`)

HTML ligatures trong room sheet: `wb_sunny, lightbulb, table_lamp, toggle_on, door_front, air, power, cooking, snowflake, kitchen, water_heater, stairs`. Icon keys trong `Models.kt`: bulb(17), sun(3), switch(2), desk(1), fan(1), stairs(2), climate→"snowflake".

- **M53** — `desk` → `Ms.desk` vs HTML `table_lamp`.
- **M54** — `fan` → fallback `bolt` (không có key) vs HTML `air`.
- **M55** — `stairs` → fallback `bolt` vs HTML `stairs`.
- **M56** — `switch` → `bolt` vs HTML `toggle_on`.
- (climate "snowflake" → `ac_unit`, không phải `snowflake` — HTML dùng cả 2 chỗ khác nhau: device snowflake vs sheet climate; cần chốt mapping.)

## L. M3ESwitch (`core/ui/.../M3ESwitch.kt:37`)

52×32 ✓, knob 16→20 ✓, vị trí (6,6)→(24,4) ✓, check icon 13dp ✓, viền 2px outline ✓, knob outline/onPrimary ✓.

- **M57** — track khi tắt: `surfaceContainerHighest` vs HTML `.tgl{background:var(--surfaceHighest)}` (sai token).
- **M58** — [BUILD BREAK] dòng ~107: `Icon(imageVector = M3EIcons.Check, ...)` trong khi `M3EIcons.Check = Ms.check` là **String** (sau migration icon) → không compile. Cả tab Home không build được cho tới khi sửa.

## M. Notification sheet (`HomeOverlays.kt:77`)

| HTML (`#nsheet` dùng chung `.sheet`; `.nfi{display:flex;gap:12px;padding:14px 4px}` + animation `.5s` **không stagger**) | Android |
|---|---|
| `.nfi` = nfic (icon 44px, **màu nền theo từng loại**: primary/tertiary/secondary/info/error/violetContainer) + nfm (`.nft` title + `.nfs` sub = time text) — **2 dòng text** | `NotifRow`: icon luôn primaryContainer; **3 dòng text** (title + body + timeText) |
| `.grab` chuẩn (4/16, outline) | `SheetGrabHandle` (dòng 463): top **12**/bottom **4**, onSurfaceVariant 40% |
| không stagger | `delay(index * 60L)` |
| empty: DOM chỉ có `#nfeed` | thêm empty state |

- **M59** — grab handle sai (12/4 + alpha vs 4/16 + outline).
- **M60** — stagger 60ms theo index; HTML không stagger.
- **M61** — row 3 dòng text vs HTML 2 dòng (title + sub=time).
- **M62** — icon nền luôn primaryContainer; HTML mỗi loại thông báo một container màu riêng (npool dòng 1566–1574).
- **M63** — empty state thêm, HTML không có.

## N. LightsSheet (`HomeOverlays.kt:228`) — sheet này **không tồn tại trong HTML** (bulbPill no-op, xem M10)

- **M64** — toàn bộ sheet là behavior thêm ngoài spec.
- **M65** — dùng `SheetGrabHandle` sai (xem M59); LazyColumn `spacedBy(10.dp)`.

## O. DeviceSearchView (`HomeOverlays.kt:277`)

| HTML (`.searchview{position:absolute;inset:0;bg:var(--surface);padding:14px 16px 0;border-radius:56px;transform:translateY(100%);transition:transform .45s var(--ease)}`) | Android Surface fullMaxSize, slide .45s emphasized ✓, clip top 56 ✓, padding top 14/start-end 16 ✓ |
|---|---|
| `.svbar{display:flex;align-items:center;gap:4px;margin-bottom:10px}`; `.iconbtn{48px;bg:surfaceLowest;.ms 24px}` | Row **không** spacedBy; nút back/clear 48dp nhưng **không** bg surfaceLowest |
| `.svlabel{11px/700/ls .6px;onSurfaceVariant;margin:10px 4px 8px}` | 11sp/Bold/0.6sp ✓ padding 4/10/8 ✓ |
| `.svchip{border 1px outline;13px/600;padding 9/16;radius 99}`; `:active{bg:secondaryContainer;radius:12}` | ✓ khớp cả pressed |
| `.svchips{gap:8px}` | spacedBy 8 ✓ |

- **M66** — `.svbar` thiếu gap **4px** (back/input/clear dính liền).
- **M67** — nút back/clear thiếu nền `surfaceLowest` (trong suốt).
- **M68** — [BUG] `MsIcon(Ms.arrow_back…)` (dòng 332) và `MsIcon(Ms.close…)` (dòng 368) **không** có size modifier → `MsIcon` fallback `minOf(maxWidth,maxHeight)` = 48dp của nút cha → icon render **48sp** thay vì 24px.

## P. FAB (`HomeFab.kt:56`)

Trigger 56dp/radius 16/primaryContainer → mở: circle/primary/icon 20dp onPrimary ✓; items pill 56dp/primaryContainer/icon 24/label 14-700 ✓; stagger 20→200ms từ item gần nút nhất ✓ (HTML `.02/.08/.14/.2s`); scrim `cs.scrim` full-alpha (HTML `--scrim:rgba(0,0,0,.6)`); vị trí right 20/bottom 108 ✓.

- **M69** — exit: Android thêm slide/scale emphasized accelerate riêng; HTML dùng chung transition (opacity .3s, transform .45s spring) cả 2 chiều.
- **M70** — shadow trigger/items `.shadow(8.dp)` vs HTML `box-shadow:var(--shadow)` = `0 12px 32px rgba(25,20,18,.10)` (light).

## Q. Navbar (`M3ERootScreen.kt`)

| HTML (dòng 751–760) | Android |
|---|---|
| `.nav{left:16;right:16;bottom:20;radius 34;bg:surfaceLowest 82%;backdrop blur 22px;shadow var(--shadow);padding:10px;display:flex}` — **không gap** giữa items | thêm `Arrangement.spacedBy(4.dp)`; **bỏ** backdrop blur; shadow 8dp; thêm `navigationBarsPadding()` |
| `.navit{flex:1 1 0;gap:3px;padding:9px 0 7px;radius 24px}`; `.on{bg:primaryContainer}` | ✓ |
| `.navit .ms{24px}`; `.on .ms{scale(1.12)}` → 27px | selected icon 27dp ✓ |
| `.navit span{11px/600}`; `.on span{700}` | 11sp ✓ / Bold ✓ |
| `.press-main{flex-grow:1.18}` / `.press-nei{flex-grow:.93}` | dùng chung `NeighborPressState` **1.45/0.82** (giá trị của `.rmm`) |

- **M71** — Row thêm gap 4dp; HTML không có gap (items bị hẹp lại).
- **M72** — thiếu `backdrop-filter:blur(22px)`.
- **M73** — shadow 8dp vs `0 12px 32px rgba(25,20,18,.10)`.
- **M74** — neighbor-press weights 1.45/0.82 vs HTML của nav **1.18/0.93**.
- **M75** — `navigationBarsPadding()` làm bottom position khác fixed 20px của HTML trên frame demo.

## R. EcoDialog (`HomeScreen.kt:371`)

| HTML (`.dialog{left:32;right:32;top:38%;bg:surfaceContainerHigh;radius 28px;padding:24px}`; `.dicon{24px;secondary;mb 16px}`; `.dtitle{24px/500/mb 12px}`; `.dtext{14px/lh 20px/mb 20px}`; `.dactions{gap:4px}`; `.dbtn{14px/700/primary;padding 10/14;radius 99}`) | Android |
|---|---|
| shape 28 ✓, surfaceContainerHigh ✓, padding 24 ✓, enter fade + scale .92 spring ✓ | icon 24dp secondary ✓; title 24sp/Medium nhưng Spacer **16dp**; text Spacer 20 ✓; actions gap 4 ✓; DialogButton 14sp/700/primary/padding 14/10 ✓ |
| `.dscrim{background:var(--scrim)}` = rgba(0,0,0,.6) | `cs.scrim` (cam light = `0x73191412` ≈ 45% nâu đen) |

- **M76** — Spacer sau icon **16dp** vs HTML `margin-bottom:12px`.
- **M77** — scrim `0x73191412` (≈45%) vs `rgba(0,0,0,.6)`.
- **M78** — dialog đặt giữa + offset y −60dp vs HTML `top:38%`.

## S. Snackbar (`HomeScreen.kt:330`)

| HTML (`.snack{left:16;right:16;bottom:104px;bg:surfaceContainerHigh;radius 20px;min-height:48px;padding:6px 6px 6px 18px;14px/500;action 14px/700/primary;padding 10/12;enter:opacity .25s + translateY(16px)→0 .35s ease}`) | Android |
|---|---|
| shape 20 ✓, surfaceContainerHigh ✓, text 14sp ✓, action 14sp/Bold/primary ✓ | padding **16/8/12** vs 18/6/6; **thiếu** min-height 48; shadow 6dp vs var(--shadow); enter = default SnackbarHost animation |

- **M79** — padding 16/8/12 vs HTML 18/6/6; thiếu min-height 48dp.
- **M80** — enter animation khác (default vs fade + slide-up custom).

## T. Vấn đề hệ thống (systematic)

- **M81** — Token `--surfaceHighest` **không tồn tại** trong Android theme (`M3EColorSchemes.kt` chỉ có `surfaceContainerHighest`); mọi card `.solcard/.batcard/.roomc/.pill/.dev` đang map sang `surfaceContainerHighest`. Giá trị trùng ở seed cam/green đã kiểm tra nhưng sai 1-1 token.
- **M82** — `MsIcon` không có default size an toàn: mọi call site thiếu `Modifier.size` sẽ render theo max constraints của cha (bug M68 là instance).
- **M83** — `pressMorphCard` **luôn** gắn `.clickable` và luôn morph radius khi nhấn, kể cả `onClick = null` (DeviceRow) — HTML nhiều row không clickable.

---

## Tổng: **83 mismatches** (M1–M83)

Gồm 1 build break (M58: `Icon(imageVector = M3EIcons.Check)` với `M3EIcons.Check` là String → không compile), 1 bug render icon 48sp (M68), còn lại là lệch spec 1-1 ở mọi cấp: spacing, typography, màu token, icon ligature, behavior/animation, hierarchy.

**Các phần KHỚP static tốt:** SolarLiveCard (≈100%), pill collapsed visuals, smode cards, FAB trigger/items + stagger, M3ESwitch (trừ M57/M58), search chips, contact chip, battery bar anatomy + wave animation, riseIn stagger timings (20/140/155/165/200/220/240/340/360+30i ms).
