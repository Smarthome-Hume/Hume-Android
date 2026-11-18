package com.smarthome.hume.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.smarthome.hume.core.ui.theme.M3ETokens

/**
 * Connected button group single-select theo spec M3E (demo rev flow card):
 * vien outline, bo pill ngoai; segment duoc chon = secondaryContainer +
 * icon check fade/scale in; nhan thi morph.
 * (Segmented button da deprecated — dung mau nay.)
 */
@Composable
fun <T> M3EConnectedButtonGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    icon: ((T) -> String)? = null,
) {
    // B-06: pill percent 50 -> Corner.Pill (999dp, cung y "tron het co").
    val pill = RoundedCornerShape(M3ETokens.Corner.Pill)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(M3ETokens.Border.Thin, MaterialTheme.colorScheme.outline, pill)
            .clip(pill),
    ) {
        options.forEach { opt ->
            val isSel = opt == selected
            val bg = if (isSel) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(bg)
                    .clickable { onSelect(opt) }
                    .padding(vertical = M3ETokens.Spacing.Md, horizontal = M3ETokens.Spacing.Sm),
            ) {
                AnimatedVisibility(
                    visible = isSel,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    MsIcon(
                        M3EIcons.Check,
                        null,
                        // B-06/B-20: icon check 18dp -> Icon.XSmall 16dp (icon nho trong group; DOI GIA TRI 18 -> 16dp).
                        modifier = Modifier.size(M3ETokens.Icon.XSmall),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                icon?.let {
                    MsIcon(
                        it(opt),
                        null,
                        modifier = Modifier
                            .size(M3ETokens.Icon.Small)
                            .padding(end = M3ETokens.Spacing.Xs),
                        tint = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = label(opt),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Ban don gian 2 lua chon kieu toggle Nang luong/Cong suat. */
@Composable
fun M3EDualToggle(
    leftLabel: String,
    rightLabel: String,
    selectedLeft: Boolean,
    onSelectLeft: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    M3EConnectedButtonGroup(
        options = listOf(true, false),
        selected = selectedLeft,
        onSelect = onSelectLeft,
        label = { if (it) leftLabel else rightLabel },
        modifier = modifier,
    )
}
