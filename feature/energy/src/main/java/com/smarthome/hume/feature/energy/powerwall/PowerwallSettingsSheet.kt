package com.smarthome.hume.feature.energy.powerwall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.M3EHeaderFade
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.M3ETokens
import kotlin.math.abs

/**
 * Sheet gioi han sac/xa NATIVE (v65) — port 1:1 iOS `PowerwallSettingsSheet.swift`.
 * Thay modal HTML trong index.html: nut setting (goc the he thong) -> JS
 * bao qua bridge "HumeAndroid" {type:"settings"} -> PowerwallScreen mo
 * sheet nay. Cau truc Y HET the pack: sheet native, GrabHandle, vuot
 * xuong de an, bo 44 (Q3 chot 18/11: bo tren chung 44dp, truoc day
 * 60dp), nen surfaceContainer, M3EHeaderFade o top, content
 * top inset 81. Noi dung = 10 dong gioi han sac/xa, v70 doc LIVE tu BMS
 * (sensor setting_* tren master qua payload sys); thieu live thi fallback
 * dung so cu (da kiem chung 19:44 16/11 khop 100%).
 *
 * v72/v73 — DETENT THEO CHIEU CAO NOI DUNG DO THAT (iOS: ~539pt, thay vi
 * 0.85 de het trang thua duoi day; detent .height dat o DIEM MO SHEET vi
 * detent trong noi dung sheet khong thang detent ngoai). Tuong duong
 * Android: ModalBottomSheet von co-size theo noi dung (khong co detent
 * 0.85 gay trang thua), nen chieu cao do duoc dung lam CAP TOI DA cho
 * content sau lan do dau tien (xem [contentHeight] + [measured]) thay vi
 * ep cung chieu cao — tranh vong lap layout -> state -> layout (iOS chi
 * cap nhat khi doi that > 0.5pt; o day cung vay, > 0.5dp). Gia tri khoi
 * dau 520.dp = uoc tinh de sheet mo dung kich thuoc ngay frame dau (nhu
 * iOS), onSizeChanged chinh lai khi do xong (chenh ~19dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerwallSettingsSheet(
    payload: PowerwallLiveData.PowerwallPayload,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val rows = remember(payload) { BatterySettings.from(payload.sys).formatted() }
    val density = LocalDensity.current

    // v72: chieu cao noi dung do that (do tu anh sim 16/11 ~539pt — o cao
    // ~78pt thay vi 73pt). Khoi dau 520.dp = uoc tinh (header 81 + luoi
    // 5 hang + bottom 24 ≈ 518pt).
    var contentHeight by remember { mutableStateOf(520.dp) }
    var measured by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = M3ETokens.Corner.SheetTop, topEnd = M3ETokens.Corner.SheetTop),
        containerColor = cs.surfaceContainer,
        dragHandle = null,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    // Sau lan do dau: khoa chieu cao toi da = chieu cao noi
                    // dung do that (tuong duong detent .height iOS). Frame
                    // dau chua co so do -> khong cap, de do dung chieu cao
                    // that cua noi dung (cap 520.dp o day se cat mat ~19dp
                    // va onSizeChanged khong bao gio do duoc 539).
                    .then(if (measured) Modifier.heightIn(max = contentHeight) else Modifier)
                    .onSizeChanged { size ->
                        val d = with(density) { size.height.toDp() }
                        if (abs(d.value - contentHeight.value) > 0.5f) {
                            contentHeight = d
                            measured = true
                        }
                    }
                    .padding(horizontal = 20.dp)
                    .padding(top = 81.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // v66: theo dung the phong — padding ngang 20 (content +
                // header) + boc grid trong VStack(spacing: 12) nhu pack/phong.
                // LazyVGrid 2 cot cua iOS xep theo hang: cot 1 = hang 0,2,4,6,8;
                // cot 2 = hang 1,3,5,7,9.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        for (i in rows.indices step 2) {
                            SettingCell(rows[i].first, rows[i].second, cs)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        for (i in 1..rows.lastIndex step 2) {
                            SettingCell(rows[i].first, rows[i].second, cs)
                        }
                    }
                }
            }
            M3EHeaderFade(
                color = cs.surfaceContainer,
                height = 81.dp,
                modifier = Modifier.align(Alignment.TopStart),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
            ) {
                PowerwallGrabHandle()
                SettingsHeader(cs, Modifier.padding(start = 28.dp, end = 20.dp))
            }
        }
    }
}

// MARK: - Header: icon + tieu de (khong co nut X — dong bang vuot xuong nhu the pack/phong).

@Composable
private fun SettingsHeader(
    cs: androidx.compose.material3.ColorScheme,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(M3ETokens.Corner.Small))
                .background(cs.primaryContainer),
        ) {
            // iOS dung SF Symbol "slider.horizontal.3"; font Material
            // Symbols subset da pin khong co glyph tune — dung bolt (da
            // co trong subset, cung nhom nang luong) thay the.
            MsIcon(Ms.bolt, null, tint = cs.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Giới hạn sạc/xả",
            // Chuan hoa 18/11: title sheet -> SheetTitle 24/29,3
            // (18 -> 24sp; iOS header nay = cardTitle 20pt — ghi nhan
            // lech; icon box 44dp ap dao nen khong vo layout).
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = M3ETokens.Text.SheetTitle.fontSize,
                lineHeight = M3ETokens.Text.SheetTitle.lineHeight,
            ),
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
        )
        Spacer(Modifier.weight(1f))
    }
}

// MARK: - Mot o gia tri (nhu valueCell cua the pack, khong icon — modal cu dung vc(null, ...) cung khong icon).

@Composable
private fun SettingCell(
    label: String,
    value: String,
    cs: androidx.compose.material3.ColorScheme,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(M3ETokens.Corner.Cell))
            .background(cs.surfaceContainerHighest) // iOS SettingsSheet:131 (extra.surfaceHighest fallback Cam = nau khi seed Indigo)
            .padding(14.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = M3ETokens.Text.BodySmall.fontSize, lineHeight = M3ETokens.Text.BodySmall.lineHeight),
            fontWeight = FontWeight.Medium,
            color = cs.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = M3ETokens.Text.StatValueLarge.fontSize, lineHeight = M3ETokens.Text.StatValueLarge.lineHeight),
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
        )
    }
}
