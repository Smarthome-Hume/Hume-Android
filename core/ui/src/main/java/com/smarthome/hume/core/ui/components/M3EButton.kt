package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.smarthome.hume.core.ui.theme.M3ETokens

/**
 * Nut chuan M3E (B-08, vong chuan hoa 18/11) — lan dau tien app co
 * component nut dung chung thay vi moi man tu dung (Button M3 o
 * LoginScreen/QrScanner, DialogButton, InsightPrimaryButton...).
 *
 * - Bien the lon (mac dinh): cao [M3ETokens.Height.ButtonLarge] 56dp,
 *   chu [M3ETokens.Text.ButtonText] 14/17,1.
 * - Bien the nho (`small = true`): cao toi thieu
 *   [M3ETokens.MinHeight.ConfirmCancelButton] 34dp, padding
 *   [M3ETokens.RoomSheet.ConfirmCancelPaddingH]/V 12/9dp, chu
 *   [M3ETokens.Text.ButtonTextSmall] 12/14,6 (nut Huy/Xac nhan trong sheet).
 * - Bo goc: lon 20dp = [M3ETokens.Corner.SheetInner] (cung ho bo
 *   expressive voi M3ETextField); nho 12dp = [M3ETokens.Corner.Small]
 *   (theo tien le InsightSecondaryChip). Chua co token Corner.Button
 *   rieng — khi token file bo sung thi doi ve do.
 * - Primary: nen primary / chu onPrimary; Secondary: nen
 *   secondaryContainer / chu onSecondaryContainer. Disabled: nen
 *   surfaceContainer, chu onSurfaceVariant mo theo Alpha.DisabledText.
 *
 * Vong nay CHI TAO component, KHONG migrate call site hien co.
 */
enum class M3EButtonVariant {
    Primary,
    Secondary,
}

@Composable
fun M3EButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: M3EButtonVariant = M3EButtonVariant.Primary,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    val textSpec = if (small) M3ETokens.Text.ButtonTextSmall else M3ETokens.Text.ButtonText
    val containerColor = when {
        !enabled -> cs.surfaceContainer
        variant == M3EButtonVariant.Primary -> cs.primary
        else -> cs.secondaryContainer
    }
    val contentColor = when {
        !enabled -> cs.onSurfaceVariant.copy(alpha = M3ETokens.Alpha.DisabledText)
        variant == M3EButtonVariant.Primary -> cs.onPrimary
        else -> cs.onSecondaryContainer
    }
    val corner = if (small) M3ETokens.Corner.Small else M3ETokens.Corner.SheetInner
    val sizeMod = if (small) {
        Modifier
            .heightIn(min = M3ETokens.MinHeight.ConfirmCancelButton)
            .padding(
                horizontal = M3ETokens.RoomSheet.ConfirmCancelPaddingH,
                vertical = M3ETokens.RoomSheet.ConfirmCancelPaddingV,
            )
    } else {
        Modifier
            .height(M3ETokens.Height.ButtonLarge)
            .padding(horizontal = M3ETokens.Spacing.Lg)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(sizeMod)
            .clip(RoundedCornerShape(corner))
            .background(containerColor)
            // Nut disabled: khong pressMorph (khong scale khi nhan).
            .then(
                if (enabled) Modifier.pressMorph(onClick = { haptic(); onClick() })
                else Modifier,
            ),
    ) {
        Text(
            text,
            fontSize = textSpec.fontSize,
            lineHeight = textSpec.lineHeight,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
        )
    }
}
