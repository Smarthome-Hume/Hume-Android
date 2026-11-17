package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lop fade gradient duoi header co dinh — port 1:1 iOS
 * M3EHeaderFade.swift (v74: dinh dac 0.95, gradient smoothstep 24 diem
 * dung, dai 72dp, neo y=0, mau = surface cua chinh trang, sheet =
 * surfaceContainer).
 *
 * Cach dung: dat M3EHeaderFade lam con GIUA trong Box(alignment = Top):
 * noi dung cuon -> M3EHeaderFade -> header co dinh (header luon ro net).
 * `height` = chieu cao dai header tinh tu mep tren man hinh (= top inset
 * noi dung da chua, da gom status bar): Home = headerHeight do that,
 * Nang luong 76, An ninh/Toi 66, sheet phong 93. Phan dac (peak 0.95)
 * phu tu dinh fade toi het (height - 72) phia tren, 72dp cuoi mo dan
 * toi trong suot.
 *
 * Khong chan cham: day chi la Box ve background, khong co clickable /
 * pointerInput — tap xuyen qua xuong noi dung ben duoi, tuong duong
 * iOS .allowsHitTesting(false); header nam tren (z cao hon) van nhan
 * tap binh thuong.
 */
@Composable
fun M3EHeaderFade(
    color: Color,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    /// Dinh mo (iOS v74: 0.95, tang tu 0.90 cua v65).
    val peak = 0.95f
    /// 24 diem dung cach deu theo smoothstep v66: 0.95*(1-(3t^2-2t^3)) —
    /// 0.95 o dinh ... 0.475 o giua ... 0.0 o day. Do doc = 0 o CA HAI dau
    /// (dao ham 6t-6t^2 cua smoothstep = 0 tai t=0 va t=1): noi muot voi
    /// phan dac peak phia tren VA voi noi dung trong suot phia duoi, nen
    /// mep duoi khong con vet cat. Chenh lon nhat giua 2 diem ke ~0.062.
    val stops = (0..<24).map { i ->
        val t = i / 23f
        val s = t * t * (3 - 2 * t) // smoothstep: 0 -> 1, doc 0 o 2 dau
        t to color.copy(alpha = peak * (1 - s))
    }
    Column(modifier.fillMaxWidth().height(height)) {
        if (height > 72.dp) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height - 72.dp)
                    .background(color.copy(alpha = peak)),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(minOf(height, 72.dp))
                .background(Brush.verticalGradient(*stops.toTypedArray())),
        )
    }
}
