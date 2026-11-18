package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.ClimateUi
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.HomeNotification
import com.smarthome.hume.core.model.RoomUi
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.DeviceIcon
import com.smarthome.hume.core.ui.components.M3EHeaderFade
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.M3ESwitch
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.rememberNeighborPress
import com.smarthome.hume.core.ui.theme.M3ETokens
import java.util.Locale

/**
 * (GAP-27) Format cong suat phong, port 1:1 iOS roomPowerText /
 * sheetPowerText (HomeRooms.swift:174-179, :401-404): <1000W hien W
 * nguyen, >=1000W hien kW 1 so le. Nguong cu 100W (user 2026-10-05)
 * khong khop iOS nen bo.
 */
private fun formatRoomPowerText(w: Double): String =
    if (w >= 1000) "%.1fkW".format(w / 1000) else "%.0fW".format(w)

/**
 * Luoi the phong theo demo rev12 (.roomc): layout DOC — nut den 48px tren,
 * ten 13.5px/700, sub 11.5px/500; padding 19px 14px (GAP-30, iOS vertical
 * 19); cham bao cua mo .rdot2;
 * :active{scale(.95); radius 20px} spring; rise stagger .36s + i*.03s.
 * (Tieu de "Phong" do HomeScreen ve — khong ve trung o day.)
 */
@Composable
fun RoomGrid(
    rooms: List<RoomUi>,
    notifications: List<HomeNotification>,
    onRoom: (RoomUi) -> Unit,
    onToggleLight: (String) -> Unit,
    // (GAP-30) Nhan 1 lan duy nhat voi TOAN BO light.* cua phong —
    // port iOS toggleRoomLights (HomeRooms.swift:241-248, 1 lenh
    // light.turn_on/off voi entity_id = danh sach). Null = caller chua
    // co method batch, fallback goi onToggleLight tung den nhu cu.
    onToggleLights: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Md)) {
        rooms.chunked(2).forEachIndexed { ri, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Md)) {
                row.forEachIndexed { ci, room ->
                    RoomCard(
                        room = room,
                        onOpen = { onRoom(room) },
                        onToggleLight = {
                            // (GAP-30) iOS lightIds (HomeRooms.swift:147-149):
                            // TOAN BO light.* cua phong; callee quyet dinh
                            // turn_on/off theo anyOn roi gui 1 lenh duy nhat.
                            val ids = room.devices.filter {
                                it.entityId.startsWith("light.")
                            }.map { it.entityId }
                            if (ids.isNotEmpty()) {
                                if (onToggleLights != null) {
                                    onToggleLights(ids)
                                } else {
                                    // Fallback tam: chua co method batch o
                                    // ViewModel/repo (nam ngoai file nay).
                                    room.devices.filter {
                                        it.entityId.startsWith("light.") &&
                                            it.isOn == room.lightOn
                                    }.forEach { onToggleLight(it.entityId) }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RoomCard(
    room: RoomUi,
    onOpen: () -> Unit,
    onToggleLight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    // Chan nested clickable: trong Compose nut con + the cha deu fire onClick
    // khi bam nut den. Nut con ghi timestamp, the cha bo qua tap trong 400ms
    // sau do (fix 2026-09-30).
    var lastLightTapMs by remember { mutableLongStateOf(0L) }
    Box(
        modifier = modifier
            .pressMorphCard(
                pressedScale = 0.95f,
                corner = M3ETokens.Corner.Card,
                pressedCorner = M3ETokens.Corner.SheetInner,
                onClick = {
                    if (SystemClock.uptimeMillis() - lastLightTapMs > 400) {
                        haptic(); onOpen()
                    }
                },
            )
            .background(
                if (room.lightOn) cs.primaryContainer
                else cs.surfaceContainerHighest,
            )
            .padding(horizontal = 14.dp, vertical = 19.dp),
    ) {
            Column {
                // Hang tren: icon phong + nhiet do sat icon.
                // Icon luon can giua nen tron 48dp; nhiet do co inset rieng 10dp.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Nut den 48px (icon phong): tat = surfaceContainer, bat = primary
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(M3ETokens.Icon.IconBoxLarge)
                            .clip(CircleShape)
                            .background(
                                if (room.lightOn) cs.primary else cs.surfaceContainer,
                            )
                            .pressMorph(pressedScale = 0.85f) {
                                lastLightTapMs = SystemClock.uptimeMillis()
                                haptic()
                                onToggleLight()
                            },
                    ) {
                        MsIcon(
                            M3EIcons.room(room.iconKey), null,
                            tint = if (room.lightOn) cs.onPrimary
                            else cs.onSurfaceVariant,
                            modifier = Modifier.size(M3ETokens.Icon.Standard),
                        )
                    }
                    // Nhiet do sat icon; do chinh xac chuoi de chon co chu lon nhat
                    // vua phan rong, tranh tran o ca nhiet do am tren the hep.
                    val temp = room.tempC
                    if (temp != null) {
                        val temperatureText = "%.1f°".format(Locale.US, temp)
                        val textMeasurer = rememberTextMeasurer()
                        val baseTemperatureStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Light,
                            // (2026-11-17) iOS tempValue khong co tnum
                            // (M3ETheme.swift:135) — bo tnum tang bien an toan.
                            fontFeatureSettings = "",
                        )
                        BoxWithConstraints(
                            contentAlignment = Alignment.CenterStart,
                            // Cung slot cao 48dp voi icon; spacer 25dp phia duoi
                            // tao cung khoang cach den tieu de cho ca icon va gia tri.
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = M3ETokens.Spacing.RowGap)
                                .height(M3ETokens.Icon.IconBoxLarge),
                        ) {
                            val maxTextWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                                maxWidth.roundToPx()
                            }
                            val temperatureFontSize = listOf(30, 28, 26, 24, 22, 20, 18, 16, 14, 12)
                                .firstOrNull { fontSize ->
                                    val candidateStyle = baseTemperatureStyle.copy(
                                        fontSize = fontSize.sp,
                                        lineHeight = M3ETokens.naturalLineHeight(fontSize.sp),
                                    )
                                    !textMeasurer.measure(
                                        text = AnnotatedString(temperatureText),
                                        style = candidateStyle,
                                        overflow = TextOverflow.Clip,
                                        softWrap = false,
                                        maxLines = 1,
                                        constraints = Constraints(maxWidth = maxTextWidthPx),
                                    ).hasVisualOverflow
                                }?.sp ?: M3ETokens.Text.BodySmall.fontSize
                            Text(
                                temperatureText,
                                style = baseTemperatureStyle.copy(
                                    fontSize = temperatureFontSize,
                                    lineHeight = M3ETokens.naturalLineHeight(temperatureFontSize),
                                ),
                                fontWeight = FontWeight.Light,
                                color = if (room.lightOn) cs.onPrimaryContainer
                                else cs.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
                // Khoang cach icon-ten phong 25dp (iOS: Spacer().frame(height: 25))
                Spacer(Modifier.height(25.dp))
                // (2026-11-17) Ten phong ghim fontScale = 1f nhu subtitle:
                // iOS Font.custom 14pt cung, khong scale theo Dynamic Type
                // (o fontScale 1,15 ten dai nhat 144,4dp > slot 140dp va
                // bat dau cuon, trong khi iOS khong bao gio cuon).
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = 1f,
                    ),
                ) {
                    com.smarthome.hume.core.ui.components.MarqueeText(
                        text = room.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (room.lightOn) cs.onPrimaryContainer
                        else cs.onSurface,
                    )
                }
                // (GAP-27) Sub luon "N thiet bi · M bat · power" khi co
                // cong suat, nhu iOS HomeRooms.swift:224-225.
                // (2026-11-17, the phong trang chinh: cong suat bi che)
                // NGUYEN NHAN DO THAT: sub la 1 Text duy nhat, maxLines=1 +
                // Ellipsis o CUOI — cong suat nam cuoi chuoi nen bat cu tran
                // nao cung an mat no truoc. Do that bang Montserrat-Medium
                // 12 (file iOS Hume/Fonts/Montserrat-Medium.ttf, cung font
                // Android tai qua Google Fonts): "5 thiết bị · 3 bật · 816W"
                // = 136.5pt; Android them tnum (bodySmall co
                // fontFeatureSettings="tnum", iOS M3EType khong co) lam
                // "816" rong them toi +4.4 (tabular = rong so 0) -> 140.9dp. Slot chu Android:
                // (411 - 18*2 - 12)/2 - 14*2 = 153.5dp — vua o fontScale
                // 1.0 (thua 12.6dp) nhung vo o fontScale >= ~1.09, trong khi
                // iOS chu 12pt CUNG (Font.custom khong scale theo Dynamic
                // Type) + slot 144pt nen van vua. Man hinh khong phai ly do:
                // S26U 411dp con RONG hon iPhone 402pt, the Android
                // (181.5dp) rong hon the iOS (172pt).
                // FIX: tach sub thanh Row — phan dem weight(1f) + Ellipsis,
                // phan cong suat co dinh, KHONG bao gio bi cat. O fontScale
                // 1.0 render giong het 1 Text cu (cung chuoi, cung style);
                // khi chu phong to, chi phan dem bi "...", cong suat van
                // hien du. Khong thu nho chu, khong cat thong tin.
                val subColor = if (room.lightOn) cs.onPrimaryContainer
                else cs.onSurfaceVariant
                // (2026-11-17, sub van bi "1 ..." — anh 4870, ban 04b900e)
                // iOS M3EType.bodySmall KHONG co tnum; Android bodySmall co
                // fontFeatureSettings="tnum" (theme toan cuc, HumeM3ETheme.kt)
                // lam so rong them (tabular = rong so 0). Do that bang
                // Montserrat-Medium 12 (fontTools, UPM 1000), slot S26U 140dp
                // (the 168dp - padding 14x2): "2 thiet bi · 1 bat · 537W"
                // tnum = 143.2dp VUOT 3.2dp -> "..." an "bat"; bo tnum =
                // 135.4dp VUA (thua 4.6dp). "7 thiet bi · 5 bat · 1,2kW":
                // 145.3 -> 138.7dp vua. Ghi de CUC BO o sub nay (style
                // copy), khong doi theme — tnum toan cuc giu cho cac cho
                // khac can so thang hang. Row tach (7f2ab6b) giu nguyen:
                // truong hop cuc doan ("9 thiet bi · 9 bat · 9999W" = 147.6dp
                // ca khi bo tnum) thi chi phan dem bi "...", cong suat van
                // hien — iOS slot 144pt cung cat chuoi nay.
                val subStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFeatureSettings = "",
                    // (2026-11-17) lineHeight tu nhien 14,6sp (= 12 x 1,219):
                    // M3 bodySmall 16dp thua 1,4dp/the so voi iOS.
                    lineHeight = M3ETokens.Text.BodySmall.lineHeight,
                )
                // (2026-11-17, LỖI 3 vòng fix 2: sub vẫn bị "N ..." — ảnh 4842)
                // iOS sub thẻ phòng là Font.custom 12pt CỨNG (M3EType.bodySmall,
                // HomeRooms.swift:223-229 + M3ETheme.swift:143: f(12, .medium) ->
                // Font.custom, không scale theo Dynamic Type), slot 144pt nên
                // "7 thiết bị · 5 bật · 1,2kW" ~139pt luôn vừa. Android bodySmall
                // là 12.sp nên scale theo fontScale hệ thống: ở fontScale > 1.0,
                // Row tách của fix 7f2ab6b giữ được công suất nhưng phần đếm
                // weight(1f) vẫn bị "..." ("5 ..." trong ảnh). Ghim fontScale =
                // 1f CHỈ cho sub này để render đúng 12sp như iOS 12pt ở mọi cỡ
                // chữ hệ thống. Không đổi chuỗi (đã là dạng ngắn "bật" như iOS),
                // không thu nhỏ chữ, không đổi layout.
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = 1f,
                    ),
                ) {
                    // (2026-11-17, sub van bi "N ..." khi cong suat >= 2,0kW)
                    // Slot 140dp; "7 thiet bi · 7 bat · 2,0kW" = 142,5dp,
                    // "4 thiet bi · 4 bat · 2,0kW" = 144,2dp ngay ca khi da
                    // bo tnum; iOS vua duoc chi nho slot 163pt. Thu chu
                    // xuong 11sp CHI khi do thay tran (pattern nhiet do):
                    // ca xau nhat 144,2 -> 132,2dp vua, ca cuc doan
                    // 147,6 -> 135,3dp vua. lineHeight giu 14,6sp ca 2 co
                    // de chieu cao the khong doi khi doi co.
                    val textMeasurer = rememberTextMeasurer()
                    val fullSub = if (room.roomPowerW > 0) {
                        "${room.deviceCount} thiết bị · ${room.devicesOn} bật · " +
                            formatRoomPowerText(room.roomPowerW)
                    } else {
                        "${room.deviceCount} thiết bị · ${room.devicesOn} bật"
                    }
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val maxWpx = with(LocalDensity.current) {
                            maxWidth.roundToPx()
                        }
                        val subFontSize = if (textMeasurer.measure(
                            text = AnnotatedString(fullSub),
                            style = subStyle,
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                            maxLines = 1,
                            constraints = Constraints(maxWidth = maxWpx),
                        ).hasVisualOverflow) M3ETokens.Text.Caption.fontSize else M3ETokens.Text.BodySmall.fontSize
                        val sizedSubStyle = subStyle.copy(fontSize = subFontSize)
                        if (room.roomPowerW > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${room.deviceCount} thiết bị · ${room.devicesOn} bật",
                                    style = sizedSubStyle,
                                    fontWeight = FontWeight.Medium,
                                    color = subColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    " · " + formatRoomPowerText(room.roomPowerW),
                                    style = sizedSubStyle,
                                    fontWeight = FontWeight.Medium,
                                    color = subColor,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        } else {
                            Text(
                                "${room.deviceCount} thiết bị · ${room.devicesOn} bật",
                                style = sizedSubStyle,
                                fontWeight = FontWeight.Medium,
                                color = subColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }

/** Tay cam keo sheet (.grab 44x5). Dung chung cho RoomSheet + cac sheet trong Overlays. */
@Composable
internal fun GrabHandle() {
    Box(
        modifier = Modifier
            .padding(top = M3ETokens.Spacing.Xs, bottom = M3ETokens.Spacing.Lg)
            .size(44.dp, 5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outline),
    )
}

/**
 * Sheet chi tiet phong theo demo rev12 (.sheet): nen surfaceContainer,
 * grab, tieu de 24px/500 + dong sub "N thiet bi · M dang bat" 12px/500.
 * Dung ModalBottomSheet cua M3 de giu day du chuc nang (keo dong, scrim...).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSheet(
    room: RoomUi,
    notifications: List<HomeNotification>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onClimateTemp: (String, Double) -> Unit,
    onHvacMode: (String, String) -> Unit,
    onToggleClimate: (String) -> Unit,
    // (Batch D, GAP-07) Trang thai tho HA + hanh dong cho the mo rong.
    entityStates: Map<String, com.smarthome.hume.core.model.HomeEntity> = emptyMap(),
    expandedActions: ExpandedActions = ExpandedActions(),
) {
    val cs = MaterialTheme.colorScheme
    // Thiet bi dang mo rong (tap don vao cum icon+ten) — 1 the mo 1 luc,
    // nho theo entityId nhu iOS expandedDevice (HomeRooms.swift).
    var expandedDeviceId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Vi tri item cua thiet bi trong LazyColumn (N-07: header da doi ra
    // dai co dinh nen khong con item header): env(0) + climate?(1) +
    // tieu de "Thiet bi"(1) + vi tri thiet bi.
    val deviceItemIndex: (String) -> Int = { id ->
        val base = 1 + (if (room.climate != null) 1 else 0) +
            (if (room.devices.isNotEmpty()) 1 else 0)
        base + room.devices.indexOfFirst { it.entityId == id }
    }
    // Mo xong cuon giu the o DAU khung nhin (iOS: delay 100ms + scrollTo
    // anchor .top + re-check expandedDevice — neo .center cat 2 dau khi
    // the cao hon man hinh, fix iOS c45e206).
    LaunchedEffect(expandedDeviceId) {
        val id = expandedDeviceId ?: return@LaunchedEffect
        delay(100)
        if (expandedDeviceId != id) return@LaunchedEffect
        val idx = deviceItemIndex(id)
        if (idx >= 0) listState.animateScrollToItem(idx)
    }
    // Thiet bi switch dang cho xac nhan tat (bao ve Zigbee mesh, port iOS 0e855dd).
    // (N-07b) Chieu cao sheet tinh TU MAN HINH (screenHeightDp * 0.85),
    // khong dung fillMaxHeight(0.85f): fillMaxHeight lay ti le theo
    // constraint noi dung ma M3 cap cho ModalBottomSheet.
    // (N-07d) Chieu cao sheet = 0.85 * screenHeightDp, KHONG tru inset nua.
    // Do lai tu anh that (ti le that 2.633 px/dp — grab 44dp = 115.7px,
    // bo 60dp = 158.0px, rong anh 1011px / man 384dp; S26U 1440x3120 @
    // density 3.75 -> man 384x832dp, khop probe SM-S948B):
    // - Anh 1011x2048 bi cat ~39.5dp tren (status bar: status_bar_height_
    //   portrait 7.54mm ~ 148px ~ 39.5dp; anh khong thay status bar) nen
    //   "top gap 93dp" tinh tu mep anh la SAI. Gap that 4848 = 223.5px /
    //   2.633 + 39.5 = 124.4dp; mo hinh: content 0.85*832 - 14.8 = 692.4
    //   + B (M3 cong them, do duoc 14.8dp) = 707.2 -> gap 124.8dp. Anh
    //   4840 khop y he: 184.5px/2.633 + 39.5 = 109.6dp vs mo hinh
    //   832 - (707.2 + 14.8) = 110.0dp. "Lech ~31dp" = 39.5 (crop tren
    //   bi bo qua) - 8.6 (sai ti le 2.46 thay vi 2.633) — khong con phan
    //   lech nao chua giai thich. May dung cu chi nen B = navRead = ~15dp
    //   (KHONG phai 48dp); vong 2 da dat 124dp, khong phai 93dp.
    // - Phep tru vong 2 (614c381) chi dung nho B (M3 doc trong Dialog)
    //   == navRead (doc o activity): doi che do nav, IME mo (mac dinh
    //   modalWindowInsets gom ca IME), hay M3 doi dinh nghia thi lech
    //   lai. Cach ben: tu nam contentWindowInsets = 0 (M3 khong cong
    //   them gi — pattern chinh thuc trong doc M3 "Use Material 3
    //   insets"), va do nav inset TRONG Dialog. RoomSheet khong co o
    //   nhap lieu nen tat IME padding la an toan. So "826dp" o comment
    //   cu khong co nguon goc do duoc (0.15*826 = 123.9dp, van ~124dp);
    //   mo hinh anh khop 832dp.
    // (fix 3d, 2026-11-17 — BAN CHOT theo anh iOS user gui 19:27)
    // The nam TREN navbar (che navbar) — dung nhu iOS; ModalBottomSheet
    // (Dialog) da dat the tren navbar, GIU nguyen, khong refactor.
    // 3 canh (2 ben + day) cach deu mep man hinh 8dp (tinh toi bounding
    // box). Bo 20dp ca 4 goc (user 20:44: bo 14dp van be so voi bo
    // man hinh, cho len 20dp). 14dp la gia tri dong tam (R_man Samsung
    // 22dp - khe 8dp) nhung nhin van be nen user chot 20dp truc quan.
    // KHONG copy bo 60pt cua iOS — man Samsung bo khac iPhone (man
    // iPhone bo ~60pt, man S26U bo ~22dp). Chieu cao the theo iOS
    // (user 19:44: the Android cao hon
    // iOS): the iOS cao 737pt/956pt = 77%, top gap 211pt/956pt = 22%
    // (do tu anh iOS 19:27) → top gap Android = 22% * 832dp = 183dp,
    // the cao 641dp = 77%. Kich thuoc the suy ra tu gia tri runtime
    // (KHONG hardcode 411/826): rong = screenW - 2*8,
    // cao = screenH - 183 - 8.
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val sheetMargin = M3ETokens.Spacing.Sm
    val topGap = M3ETokens.Height.SheetTopGap
    val cardH = screenH - topGap - sheetMargin
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // (fix 3d) Container M3 TRONG SUOT + hinh chu nhat: the NHIN THAY
        // ve BEN TRONG content (bo 20dp ca 4 goc, nen surfaceContainer).
        // Ly do: M3 neo day container vao mep duoi man hinh va container
        // full-width — muon the noi cach mep 8dp thi container phai trong
        // suot, the that nam trong. Shape container de chu nhat co tinh:
        // neu de bo 20dp, Surface cat content theo shape se cat vao goc
        // cua the. Scrim + vuot dong cua M3 giu nguyen.
        shape = RectangleShape,
        containerColor = Color.Transparent,
        // (N-07d) Tu kiem soat inset: M3 khong cong them bottom inset
        // (mac dinh modalWindowInsets = bottom insets, ap qua
        // windowInsetsPadding duoi content) — ly do xem (N-07d) o tren.
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        // (N-07) Khong dung dragHandle slot cua M3: GrabHandle nam trong
        // dai header co dinh de fade 93dp phu kin tu dinh sheet nhu iOS
        // (GrabHandle + header trong VStack co dinh, HomeRooms.swift:376-377).
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(screenH - topGap),
        ) {
            // The noi: rong = screenW - 2*8, cao = screenH - 124 - 8,
            // bo 20dp CA 4 GOC (user chot 20:44, xem (fix 3d) o tren),
            // nen surfaceContainer.
            Box(
                Modifier
                    .padding(horizontal = sheetMargin)
                    .fillMaxWidth()
                    .height(cardH)
                    .clip(RoundedCornerShape(M3ETokens.Corner.SheetInner))
                    .background(cs.surfaceContainer),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = M3ETokens.Spacing.Xl),
                    // (N-07) Top inset 93dp (= dai header 25 GrabHandle +
                    // 56 header + 12 gap) de tile dau tien bat dau duoi
                    // header co dinh (iOS HomeRooms.swift:355).
                    // Bottom trong the = 0dp: noi dung cuoi cach day the
                    // 24dp nho item Spacer cuoi list.
                    contentPadding = PaddingValues(top = 93.dp, bottom = 0.dp),
                    // (2026-11-18) Khoi phuc: khe doc giua cac the con 8->12dp (iOS 12pt).
                    verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.CardGap),
                ) {
                // 2 tile nhiet do / do am (.shenv .tile: khong press morph)
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.RowGap)) {
                        EnvTile(
                            icon = Icons.Outlined.Thermostat, label = "Nhiệt độ",
                            value = room.tempC?.let { "%.1f°".format(Locale.US, it) } ?: "—",
                            container = cs.primaryContainer,
                            onContainer = cs.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        EnvTile(
                            icon = Icons.Outlined.WaterDrop, label = "Độ ẩm",
                            value = room.humidityPct?.let { "%.0f%%".format(it) } ?: "—",
                            container = LocalHumeExtraColors.current.infoContainer,
                            onContainer = LocalHumeExtraColors.current.onInfoContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // Dieu hoa
                room.climate?.let { c ->
                    item { ClimateCard(c, onClimateTemp, onHvacMode, onToggleClimate) }
                }
                // Thiet bi
                if (room.devices.isNotEmpty()) {
                    item {
                        Text(
                            "Thiết bị",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = M3ETokens.Text.Body.fontSize,
                                lineHeight = M3ETokens.Text.Body.lineHeight,
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                            // (2026-11-18) Khoi phuc: khe tren tieu de 2->4dp (iOS 4pt).
                            modifier = Modifier.padding(top = M3ETokens.RoomSheet.DevicesTitleGap),
                        )
                    }
                    items(room.devices, key = { it.entityId }) { d ->
                        DeviceRow(
                            d,
                            contactOpen = contactOpenForDevice(d.iconKey, d.label, notifications),
                            onToggle = { onToggle(d.entityId) },
                            expanded = expandedDeviceId == d.entityId,
                            onToggleExpand = {
                                // Mo/thu dung curve the an ninh: mo 450ms,
                                // dong 700ms (iOS spring 0.45 / 0.7).
                                val willOpen = expandedDeviceId != d.entityId
                                expandedDeviceId = if (willOpen) d.entityId else null
                            },
                            expandedContent = {
                                RoomExpandedContent(d, entityStates, expandedActions)
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
                }
                // (N-07) Fade gradient kieu Zalo o dai top
                // (M3EHeaderFade, iOS HomeRooms.swift:376-377).
                M3EHeaderFade(
                    color = cs.surfaceContainer,
                    height = 93.dp,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                // (N-07) Dai top co dinh — GrabHandle + header, noi dung
                // cuon ben duoi tan dan qua lop fade (iOS v62/v63).
                Column(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GrabHandle()
                    Column(
                        Modifier
                            .fillMaxWidth()
                            // (N-07) Bo the 6dp nen goc tren bo nhe
                            // hon — doi header sang phai 8dp cho khoi sat
                            // mep cong (iOS v59).
                            .padding(start = M3ETokens.Spacing.Sm)
                            .padding(horizontal = M3ETokens.Spacing.Xl),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MsIcon(
                                M3EIcons.room(room.iconKey), null,
                                tint = cs.primary,
                                modifier = Modifier.size(30.dp),
                            )
                            Spacer(Modifier.width(M3ETokens.Spacing.RowGap))
                            Text(
                                room.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = M3ETokens.Text.SheetTitle.fontSize,
                                    lineHeight = M3ETokens.Text.SheetTitle.lineHeight,
                                ),
                                fontWeight = FontWeight.Medium,
                                color = cs.onSurface,
                            )
                        }
                        // (2026-11-17) Sub sheet ap cung subStyle (bo tnum,
                        // lineHeight 14,6sp) + ghim fontScale = 1f nhu sub
                        // the phong, cho dong nhat (slot sheet ~328dp nen
                        // chi ket khi fontScale >= ~1,8, khong phai viec gap).
                        CompositionLocalProvider(
                            LocalDensity provides Density(
                                density = LocalDensity.current.density,
                                fontScale = 1f,
                            ),
                        ) {
                            Text(
                                // (GAP-27) Sheet luon kem power, ke ca 0W —
                                // iOS HomeRooms.swift:401-404.
                                "${room.deviceCount} thiết bị · ${room.devicesOn} đang bật · " +
                                    formatRoomPowerText(room.roomPowerW),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFeatureSettings = "",
                                    lineHeight = M3ETokens.Text.BodySmall.lineHeight,
                                ),
                                fontWeight = FontWeight.Medium,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(top = M3ETokens.Spacing.Xs, bottom = M3ETokens.Spacing.Xs),
                            )
                        }
                    }
                }
            }
            // (fix 3c) Spacer TRONG SUOT duoi the: day the cach mep duoi
            // dung 16dp — day moi la co che tao margin day (M3 neo day
            // container vao mep duoi). Vuot dong van hoat dong ca tren
            // vung nay vi no nam trong container cua sheet.
            Spacer(Modifier.height(sheetMargin))
        }
    }
}

@Composable
private fun EnvTile(
    icon: ImageVector,
    label: String,
    value: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(M3ETokens.Corner.EnvTile)) // NGOAI LE: bo 30dp giu nguyen (iOS 24pt, audit 11.5)
            .background(cs.surfaceContainerHighest)
            // (2026-11-18) Giam 6dp: padding EnvTile 14->13dp (the 13+44+13 = 70dp). Bo 30dp GIU NGUYEN.
            .padding(M3ETokens.RoomSheet.EnvTilePadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // (2026-11-18) Giam 6dp: vong tron icon EnvTile 48->44dp, icon 24->22dp (token).
                    .size(M3ETokens.Icon.EnvTileBox)
                    .clip(CircleShape)
                    .background(container),
            ) {
                Icon(icon, null, tint = onContainer, modifier = Modifier.size(M3ETokens.Icon.EnvTile))
            }
            Spacer(Modifier.width(M3ETokens.Spacing.Md))
            Column {
                Text(
                    value,
                    // (GAP-30) EnvTile value titleMedium, nhu iOS
                    // HomeRooms.swift:490 (comment iOS :473 ghi titleLarge
                    // la sai so voi code).
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = M3ETokens.Text.StatValue.fontSize,
                        lineHeight = M3ETokens.Text.StatValue.lineHeight,
                    ),
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface,
                )
                Text(
                    label,
                    // (Viec 5) lineHeight tu nhien thay vi M3 16sp
                    // (iOS bodySmall 12pt tu nhien ~14,6): chi dong nhat
                    // chu, khong doi chieu cao the (icon 48dp ap dao).
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = M3ETokens.Text.BodySmall.fontSize,
                        lineHeight = M3ETokens.Text.BodySmall.lineHeight,
                    ),
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    // Khe nhan->gia tri 2->3dp = 0,20 x 16sp (Ratio.LabelToValue).
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

private val hvacLabels = mapOf(
    "cool" to "Làm lạnh",
    "heat" to "Sưởi",
    "auto" to "Tự động",
    "fan_only" to "Quạt",
    "dry" to "Hút ẩm",
)

/**
 * The dieu hoa theo demo rev12: .ac-top (stepper + nut nguon, khong title)
 * + .ac-modes chua cum .rmm. Tat dieu hoa -> stepper + modes mo di (opacity .35).
 */
@Composable
private fun ClimateCard(
    c: ClimateUi,
    onTemp: (String, Double) -> Unit,
    onMode: (String, String) -> Unit,
    onToggle: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val target = c.targetTemp ?: 26.0
    val dim = if (c.isOn) 1f else 0.35f
    val haptic = rememberHaptic()
    // .ac-top: surfaceHighest, bo 24px, padding 14px 16px
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(dim)
            .clip(RoundedCornerShape(M3ETokens.Corner.ClimateBox))
            .background(cs.surfaceContainerHighest)
            // (2026-11-18) Khoi phuc: padding doc 8->14dp, ngang 12->16dp (iOS 14/16pt).
            // Giam 6dp vong 18/11: thu cum stepper 54->48dp + nut nguon 54->48dp, GIU padding 14dp (hop 14+48+14 = 76dp).
            .padding(horizontal = M3ETokens.RoomSheet.ClimatePaddingH, vertical = M3ETokens.RoomSheet.ClimatePaddingV),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Stepper: - | nhiet do | + (nen rieng, bo "Muc tieu")
            // iOS HomeRooms.swift:535 HStack(spacing: 12), :549 .padding(6),
            // :551 corner 18 — Android cu: khong spacing, padding 4, bo 16.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                // (2026-11-18) Khoi phuc: khe stepper 8->12dp, padding 4->6dp (iOS 12/6pt).
                horizontalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.StepperGap),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(cs.surfaceContainer)
                    .padding(M3ETokens.RoomSheet.StepperPadding),
            ) {
                StepperButton(Icons.Outlined.Remove, enabled = c.isOn) {
                    haptic()
                    onTemp(c.entityId, (target - 1).coerceIn(16.0, 31.0))
                }
                Text(
                    // (GAP-28) Port iOS HomeRooms.swift:539-542: "21°C" font titleLarge.
                    // (Viec 5) lineHeight tu nhien thay vi M3 28sp
                    // (iOS titleLarge 22pt tu nhien ~26,8): chi dong nhat
                    // chu, khong doi chieu cao the (nut stepper 42dp ap dao).
                    "%.0f°C".format(target),
                    style = MaterialTheme.typography.titleLarge.copy(
                        lineHeight = M3ETokens.naturalLineHeight(22.sp),
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = M3ETokens.RoomSheet.TempPaddingH),
                )
                StepperButton(Icons.Outlined.Add, enabled = c.isOn) {
                    haptic()
                    onTemp(c.entityId, (target + 1).coerceIn(16.0, 31.0))
                }
            }
            // Nut nguon tron 54px: bat = primaryContainer, tat = surfaceContainer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // (2026-11-18) Giam 6dp: nut nguon dieu hoa 54->48dp, icon 26->24dp (token).
                    .size(M3ETokens.Icon.ClimatePowerButton)
                    .clip(CircleShape)
                    .background(if (c.isOn) cs.primaryContainer else cs.surfaceContainer)
                    .pressMorph(pressedScale = 0.88f) {
                        haptic()
                        onToggle(c.entityId)
                    },
            ) {
                Icon(
                    Icons.Outlined.PowerSettingsNew, null,
                    tint = if (c.isOn) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    modifier = Modifier.size(M3ETokens.Icon.ClimatePowerIcon),
                )
            }
        }
    }
    // (2026-11-18) Khoi phuc: khe 2 hop dieu hoa 6->10dp (iOS 10pt).
    Spacer(Modifier.height(M3ETokens.RoomSheet.ClimateGap))
    // .ac-modes: surfaceHighest, bo 24px, padding 14px
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(dim)
            .clip(RoundedCornerShape(M3ETokens.Corner.ClimateBox))
            .background(cs.surfaceContainerHighest)
            .padding(M3ETokens.RoomSheet.ClimatePaddingV),
    ) {
        ClimateModeGroup(c = c, enabled = c.isOn, onMode = onMode)
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            // (GAP-28) Port iOS HomeRooms.swift:580-592: nut stepper tron 42pt, icon 22.
            // (2026-11-18) Giam 6dp: nut 42->36dp, icon 22->20dp (token); padding cum 6dp giu -> cum 36+12 = 48dp.
            .size(M3ETokens.Icon.ClimateStepperButton)
            .clip(CircleShape)
            .background(cs.surfaceContainerHighest)
            .pressMorph(pressedScale = 0.85f, onClick = if (enabled) onClick else null),
    ) {
        Icon(
            icon, null,
            tint = cs.onSurface,
            modifier = Modifier.size(M3ETokens.Icon.ClimateStepperIcon),
        )
    }
}

/**
 * Cum che do dieu hoa (.ac-modes .rmm): khong vien, chu 12.5px/700,
 * padding 14px 6px, bo 18px; chon = primaryContainer, KHONG check;
 * neighbor-press (nhan: 1.45, ke ben: 0.82), :active scale(.9) spring.
 */
@Composable
private fun ClimateModeGroup(
    c: ClimateUi,
    enabled: Boolean,
    onMode: (String, String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    val modes = c.modes.filter { it in hvacLabels }.takeIf { it.isNotEmpty() }
        ?: listOf("cool", "heat", "auto", "fan_only")
    val selectedMode = c.hvacMode.takeIf { it in modes } ?: modes.first()
    val np = rememberNeighborPress(modes.size)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        modes.forEachIndexed { i, m ->
            val sel = m == selectedMode
            var pressed by remember { mutableStateOf(false) }
            val w by animateFloatAsState(
                targetValue = np.weightFor(i),
                animationSpec = tween(350, easing = M3EMotion.spring),
                label = "rmmW",
            )
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.9f else 1f,
                animationSpec = tween(300, easing = M3EMotion.spring),
                label = "rmmScale",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(w)
                    // (2026-11-18) Giam 6dp: chip che do 44->38dp (token); hop modes 14+38+14 = 66dp.
                    .height(M3ETokens.MinHeight.ModeChip)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (sel) cs.primaryContainer else cs.surfaceContainer)
                    .pointerInput(i, enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                np.press(i)
                                tryAwaitRelease()
                                pressed = false
                                np.release()
                            },
                            onTap = {
                                haptic()
                                onMode(c.entityId, m)
                            },
                        )
                    }
                    .padding(horizontal = 6.dp),
            ) {
                Text(
                    hvacLabels[m] ?: m,
                    // (Viec 5) lineHeight tu nhien thay vi M3 16sp
                    // (iOS labelSmall 11pt tu nhien ~13,4): chi dong nhat
                    // chu, khong doi chieu cao the (nut cao co dinh 44dp).
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = M3ETokens.Text.Caption.fontSize,
                        lineHeight = M3ETokens.Text.Caption.lineHeight,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Kiem tra thiet bi co can xac nhan inline khi tat khong (GAP-10, port
 * 1:1 iOS HomeRooms.swift:858): MOI switch (switch.*) deu can xac nhan
 * vi switch Zigbee la router mesh (user 2026-10-09); den (light.*)
 * khong anh huong mesh nen toggle truc tiep.
 */
private fun needsConfirmOff(d: DeviceUi): Boolean {
    return d.entityId.startsWith("switch.")
}

/**
 * 1 hang thiet bi theo demo rev12 (.dev): gap 13px, surfaceHighest,
 * bo 28px -> 18px khi nhan, padding 14px 16px;
 * icon 48px: tat = surfaceHigh, bat = primaryContainer;
 * ten 14px/700, trang thai 12px/500;
 * cam bien cua: chip .cchip ("DONG" surfaceContainer / "MO" errorContainer,
 * 11px/800) thay switch, sub "Dang dong"/"Dang mo".
 */
@Composable
fun DeviceRow(
    d: DeviceUi,
    onToggle: () -> Unit,
    contactOpen: Boolean? = null,
    modifier: Modifier = Modifier,
    // Ten phong hien tren dong trang thai (search, 2026-09-30): null = khong hien.
    roomLabel: String? = null,
    // (Batch D, GAP-07) The mo rong: chi RoomSheet truyen 3 param nay.
    expanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    expandedContent: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    // Xac nhan tat switch inline (port iOS HomeDeviceRow, user 2026-10-09):
    // switch Zigbee la router mesh nen tat can xac nhan, hien 2 nut ngay
    // trong the thay vi popup.
    var isConfirming by remember { mutableStateOf(false) }
    // (Batch D, GAP-29) Switch mo dan bang opacity + scale 0.7 khi chuyen
    // sang cum xac nhan (iOS spring 0.3/0.85).
    val switchAlpha by animateFloatAsState(
        targetValue = if (isConfirming) 0f else 1f,
        animationSpec = tween(350, easing = M3EMotion.spring),
        label = "confirmSwitchAlpha",
    )
    val switchScale by animateFloatAsState(
        targetValue = if (isConfirming) 0.7f else 1f,
        animationSpec = tween(350, easing = M3EMotion.spring),
        label = "confirmSwitchScale",
    )
    // Column thay cho Row cu: hang header + slot noi dung mo rong trong
    // CUNG the, cung nen surfaceContainerHighest bo 28dp (iOS :814).
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressMorphCard(
                pressedScale = 1f,
                corner = M3ETokens.Corner.Card,
                pressedCorner = 18.dp,
                onClick = null,
            )
            .background(cs.surfaceContainerHighest)
            // (2026-11-18) Giam 6dp: padding doc DeviceRow 14->13dp (the 13+44+13 = 70dp; ngang 16 giu).
            .padding(horizontal = M3ETokens.Spacing.Lg, vertical = M3ETokens.RoomSheet.DeviceRowPaddingV),
    ) {
        // (GAP-29) Tap than the (hang header, ngoai cum icon+ten) huy
        // xac nhan — port iOS HomeRooms.swift:833-837. Tap vao cum
        // icon+ten do clickable con xu ly (huy hoac mo the); switch khi
        // dang xac nhan bi disable (iOS allowsHitTesting(false)) nen
        // tap roi xuong day va huy xac nhan; khi khong xac nhan thi
        // handler nay khong lam gi, switch van nhan tap binh thuong.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // iOS HomeRooms.swift:737 HStack(spacing: 13) ngoai (cum
            // icon+ten -> trailing switch/chip) — Android cu: khong spacing.
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.clickable {
                if (isConfirming) isConfirming = false
            },
        ) headerRow@ {
            // Vung tap mo/thu: CHI cum icon+ten (khong gom vung switch,
            // de bam switch khong bi hieu thanh mo the). Dang xac nhan
            // tat thi tap nay CHI huy xac nhan, khong mo the (GAP-29).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (isConfirming) {
                            isConfirming = false
                        } else {
                            onToggleExpand?.invoke()
                        }
                    },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        // (2026-11-18) Giam 6dp: icon thiet bi 48->44dp, glyph 24->22dp
                        // (token rieng RoomSheet.DeviceRowIconBox/Icon — IconBoxLarge/Standard goc dung chung, khong doi).
                        .size(M3ETokens.RoomSheet.DeviceRowIconBox)
                        .clip(CircleShape)
                        .background(
                            if (d.isOn) cs.primaryContainer
                            else cs.surfaceContainer, // iOS HomeRooms.swift:556 (extra.surfaceHigh fallback Cam = nau khi seed Indigo/Lime)
                        ),
                ) {
                    DeviceIcon(
                        d.iconKey, null,
                        tint = if (d.isOn) cs.onPrimaryContainer
                        else cs.onSurfaceVariant,
                        modifier = Modifier.size(M3ETokens.RoomSheet.DeviceRowIcon),
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        d.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = M3ETokens.Text.Body.fontSize,
                            lineHeight = M3ETokens.Text.Body.lineHeight,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                    )
                    val powerW = d.powerW
                    val roomSuffix =
                        if (!roomLabel.isNullOrBlank()) " · $roomLabel" else ""
                    Text(
                        when {
                            contactOpen == true -> "Đang mở"
                            contactOpen == false -> "Đang đóng"
                            d.isOn -> "Đang bật"
                            else -> "Đang tắt"
                        } + (if (d.isOn && powerW != null && powerW > 0)
                            " · ${if (powerW >= 1000) "%.1f kW".format(powerW / 1000) else "%.0f W".format(powerW)}"
                        else "") + roomSuffix,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = M3ETokens.Text.BodySmall.fontSize,
                            lineHeight = M3ETokens.Text.BodySmall.lineHeight,
                        ),
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurfaceVariant,
                        // Khe ten->trang thai 2->3dp = 0,20 x 14sp (Ratio.LabelToValue).
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            if (contactOpen != null) {
                // Chip contact .cchip
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (contactOpen) cs.errorContainer
                            else cs.surfaceContainer,
                        )
                        // (2026-11-18) Khoi phuc: padding doc chip 4->7dp (chip 21,4->27,4dp, iOS 27,4pt);
                        // token MinHeight.OpenCloseChip da duoc noi day qua heightIn(min) o duoi.
                        .heightIn(min = M3ETokens.MinHeight.OpenCloseChip)
                        .padding(horizontal = 14.dp, vertical = M3ETokens.RoomSheet.OpenCloseChipPaddingV),
                ) {
                    Text(
                        if (contactOpen) "MỞ" else "ĐÓNG",
                        // (Viec 5) lineHeight tu nhien thay vi M3 16sp
                        // (iOS labelSmall 11pt tu nhien ~13,4): chip
                        // 30dp -> 27,4dp nhu iOS; khong doi chieu cao the
                        // (icon 48dp ap dao).
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = M3ETokens.Text.ChipText.fontSize,
                            lineHeight = M3ETokens.Text.ChipText.lineHeight,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = if (contactOpen) cs.onErrorContainer
                        else cs.onSurfaceVariant,
                    )
                }
            } else {
                // Trailing: switch hoac 2 nut xac nhan (overlay — switch
                // mo dan thay vi bien mat dot ngot, nut hien co animation).
                Box(contentAlignment = Alignment.CenterEnd) {
                    Box(
                        modifier = Modifier
                            .alpha(switchAlpha)
                            .graphicsLayer(scaleX = switchScale, scaleY = switchScale),
                    ) {
                        M3ESwitch(
                            checked = d.isOn,
                            // (GAP-29) Switch khong nhan hit khi dang xac
                            // nhan — iOS allowsHitTesting(!isConfirming) —
                            // de tap roi xuong tap handler muc the va huy
                            // xac nhan thay vi bi switch nuot.
                            enabled = !isConfirming,
                            onCheckedChange = {
                                if (isConfirming) return@M3ESwitch
                                haptic()
                                // Switch (switch.*) can xac nhan khi tat —
                                // router mesh (user 2026-10-09)
                                if (needsConfirmOff(d) && d.isOn) {
                                    isConfirming = true
                                } else {
                                    onToggle()
                                }
                            },
                        )
                    }
                    // Receiver tuong minh: AnimatedVisibility o day nam trong
                    // Box (BoxScope) long trong Row — overload RowScope chi
                    // goi duoc qua receiver hien, khong goi kieu implicit.
                    this@headerRow.AnimatedVisibility(
                        visible = isConfirming,
                        enter = fadeIn(tween(350, easing = M3EMotion.spring)) +
                            scaleIn(tween(350, easing = M3EMotion.spring)),
                        exit = fadeOut(tween(350, easing = M3EMotion.spring)) +
                            scaleOut(tween(350, easing = M3EMotion.spring)),
                    ) {
                        // 2 nut xac nhan inline (thay popup, user 2026-10-09)
                        Row(horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Sm)) {
                            // Nut Huy
                            // iOS HomeRooms.swift:805 .frame(minHeight: 40)
                            // — Android cu: labelMedium 12/16 + padding 20
                            // = 36dp, thap hon iOS 4dp.
                            // (2026-11-18) Khoi phuc: minHeight 32->40dp, padding doc 6->10dp (token).
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .heightIn(min = M3ETokens.MinHeight.ConfirmCancelButton)
                                    .clip(CircleShape)
                                    .background(cs.surfaceContainerHigh)
                                    .clickable { isConfirming = false }
                                    .padding(horizontal = M3ETokens.RoomSheet.ConfirmCancelPaddingH, vertical = M3ETokens.RoomSheet.ConfirmCancelPaddingV),
                            ) {
                                Text(
                                    "Hủy",
                                    // (Viec 5) lineHeight tu nhien thay vi M3 16sp
                                    // (iOS labelMedium 12pt tu nhien ~14,6): noi
                                    // dung 36dp -> 34,6dp; nut van minHeight 40dp
                                    // nhu iOS nen khong doi chieu cao nut.
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = M3ETokens.Text.ButtonTextSmall.fontSize,
                                        lineHeight = M3ETokens.Text.ButtonTextSmall.lineHeight,
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onSurface,
                                    maxLines = 1,
                                )
                            }
                            // Nut Xac nhan (do)
                            // iOS HomeRooms.swift:819 .frame(minHeight: 40).
                            // (2026-11-18) Khoi phuc: minHeight 32->40dp, padding doc 6->10dp (token).
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .heightIn(min = M3ETokens.MinHeight.ConfirmCancelButton)
                                    .clip(CircleShape)
                                    .background(cs.error)
                                    .clickable {
                                        isConfirming = false
                                        onToggle()
                                    }
                                    .padding(horizontal = M3ETokens.RoomSheet.ConfirmCancelPaddingH, vertical = M3ETokens.RoomSheet.ConfirmCancelPaddingV),
                            ) {
                                Text(
                                    "Xác nhận",
                                    // (Viec 5) lineHeight tu nhien thay vi M3 16sp
                                    // (iOS labelMedium 12pt tu nhien ~14,6): nhu
                                    // nut Huy — nut van minHeight 40dp nhu iOS.
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = M3ETokens.Text.ButtonTextSmall.fontSize,
                                        lineHeight = M3ETokens.Text.ButtonTextSmall.lineHeight,
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onError,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        // Slot noi dung mo rong (GAP-07): mo 450ms / dong 700ms theo
        // dung curve the an ninh iOS (spring 0.45 mo / 0.7 dong).
        if (expandedContent != null) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(450, easing = M3EMotion.spring)) +
                    fadeIn(tween(450, easing = M3EMotion.spring)),
                exit = shrinkVertically(tween(700, easing = M3EMotion.spring)) +
                    fadeOut(tween(700, easing = M3EMotion.spring)),
            ) {
                // (2026-11-18) Khoi phuc: khe tren slot mo rong 6->12dp (iOS 12pt).
                Column(modifier = Modifier.padding(top = M3ETokens.RoomSheet.ExpandedGap)) {
                    expandedContent()
                }
            }
        }
    }
}

