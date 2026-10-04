package com.smarthome.hume.feature.energy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import kotlinx.coroutines.launch

/**
 * Thẻ gợi ý tiết kiệm điện — port iOS `InsightsCard` (EnergyConsumeTab+Cards.swift).
 *
 * - Style đúng M3ECard như thẻ gợi ý trang Home (mục 17):
 *   bo 28dp, padding 16dp, nền tertiaryContainer.
 * - Mỗi thẻ hỗ trợ NHIỀU nút action (mục 15): nút chính bên phải (kiểu Home),
 *   nút phụ dạng chip nhỏ dưới dòng mô tả.
 * - Chiều cao co giãn theo nội dung (mục 18): HorizontalPager tự đo theo page,
 *   không fix cứng chiều cao.
 */
@Composable
fun EnergyInsightsCard(
    state: EnergyUiState,
    onInsightAction: (InsightAction) -> Unit,
    risePlayed: MutableSet<String>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    // Rule-based, thuần túy — tính lại khi state đổi (rẻ, không cần cache phức tạp).
    val insights = remember(state) { EnergyInsightsManager.generateInsights(state) }
    val pagerState = rememberPagerState(pageCount = { insights.size })

    M3ECard(
        modifier = modifier.riseOnce("cons-insights", 400, risePlayed),
        shape = RoundedCornerShape(28.dp),
        contentPadding = 16.dp,
        containerColor = cs.tertiaryContainer,
    ) {
        Column {
            if (insights.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    MsIcon(
                        Ms.check, null,
                        tint = LocalHumeExtraColors.current.success,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Mọi thứ đang tối ưu!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onTertiaryContainer,
                    )
                }
            } else {
                // Pager cao động theo nội dung từng gợi ý (không fix height).
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    val insight = insights[page]
                    InsightRow(
                        insight = insight,
                        onAction = onInsightAction,
                    )
                }
                if (insights.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        insights.forEachIndexed { i, _ ->
                            val selected = i == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (selected) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) cs.onTertiaryContainer
                                        else cs.onTertiaryContainer.copy(alpha = 0.35f),
                                    )
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(i) }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightRow(
    insight: EnergyInsight,
    onAction: (InsightAction) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    val sevColor = when (insight.severity) {
        InsightSeverity.Tip -> LocalHumeExtraColors.current.success
        InsightSeverity.Warning -> cs.error
        InsightSeverity.Info -> cs.primary
    }
    // Nút chính (kiểu Home) + các nút phụ
    val primary = insight.actions.firstOrNull { it.isPrimary } ?: insight.actions.firstOrNull()
    val secondary = insight.actions.filter { it != primary }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        // Icon box 40dp, màu theo severity, nền 12% (giống iOS)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sevColor.copy(alpha = 0.12f)),
        ) {
            MsIcon(
                insight.glyph.ifBlank { Ms.auto_awesome }, null,
                tint = sevColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Text(
                insight.title,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = buildString {
                append(insight.detail)
                if (insight.saving != null) append(" ${insight.saving}")
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onTertiaryContainer.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            // Các nút phụ: chip nhỏ dưới mô tả (mục 15)
            if (secondary.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    secondary.forEach { act ->
                        InsightSecondaryChip(act = act) {
                            haptic()
                            onAction(act)
                        }
                    }
                }
            }
        }
        if (primary != null) {
            Spacer(Modifier.width(12.dp))
            InsightPrimaryButton(act = primary) {
                haptic()
                onAction(primary)
            }
        }
    }
}

/** Nút chính kiểu Home: nền onTertiaryContainer, bo 14dp. */
@Composable
private fun InsightPrimaryButton(
    act: InsightAction,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .pressMorph(
                pressedScale = 0.9f,
                onClick = onClick,
            )
            .clip(RoundedCornerShape(14.dp))
            .background(cs.onTertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            act.label,
            style = MaterialTheme.typography.labelLarge,
            color = cs.tertiaryContainer,
            maxLines = 1,
        )
    }
}

/** Nút phụ: chip tonal nhỏ. */
@Composable
private fun InsightSecondaryChip(
    act: InsightAction,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pressMorph(pressedScale = 0.92f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(cs.onTertiaryContainer.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (act.glyph.isNotBlank()) {
            MsIcon(
                act.glyph, null,
                tint = cs.onTertiaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            act.label,
            style = MaterialTheme.typography.labelMedium,
            color = cs.onTertiaryContainer,
            maxLines = 1,
        )
    }
}
