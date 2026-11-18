package com.smarthome.hume.feature.energy

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Mau dot theo trang thai inverter Solis — HAM CHUNG (A-12, 18/11).
 * Dung chung cho EnergyScreen (solisStatusColor cu) va EnergyFlowCard
 * (solisStatusVn cu); xoa map hardcode trung lap o ca 2 file.
 *
 * Q7 (user chot): "normal operation" -> mau chu mac dinh cua theme
 * ([ColorScheme.onSurface]), DOI KHOI do #EF4444 — truoc day "hoat dong
 * binh thuong" va "loi" cung 1 mau do. Gia tri con lai giu nguyen:
 * generating #F5B80B, no grid #8B5CF6, waiting #2DD4BF,
 * bypass/running #EC72A8, bypass/sync #A16207, fault #EF4444,
 * standby/default #22C55E.
 *
 * onSurface cua seed Indigo (M3EColorSchemes.kt, colorSchemeForCustom):
 * light #191412, dark #EDE8E2 — lay dong qua `cs` nen tu theo theme.
 */
fun inverterStatusColor(status: String, cs: ColorScheme): Color {
    return when (status.lowercase().trim()) {
        "generating" -> Color(0xFFF5B80B) // vang
        "no grid" -> Color(0xFF8B5CF6) // tim
        "waiting" -> Color(0xFF2DD4BF) // teal
        "normal operation" -> cs.onSurface // mau chu mac dinh cua theme (Q7)
        "bypass / inverting / running" -> Color(0xFFEC72A8) // hong
        "bypass / inverting / synchronize" -> Color(0xFFA16207) // nau
        "fault" -> Color(0xFFEF4444) // do
        else -> Color(0xFF22C55E) // xanh la mac dinh (standby / khong ro)
    }
}
