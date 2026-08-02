package com.smarthome.hume.ui.root

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HumeTab
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.ui.energy.EnergyScreen
import com.smarthome.hume.ui.home.HomeScreen
import com.smarthome.hume.ui.login.LoginScreen
import com.smarthome.hume.ui.profile.ProfileScreen
import com.smarthome.hume.ui.security.SecurityScreen
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/** The only tabs the app ships with. The AI butler tab was removed. */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)

/**
 * One UI 8.5 bottom navigation metrics.
 *
 * Samsung docks the bar to the bottom edge (it is not a floating pill), gives
 * every item the same width, and marks the active one with a rounded
 * "selected" indicator behind the icon only, with the label sitting below it.
 */
private val BarHeight = 64.dp
private val IndicatorWidth = 64.dp
private val IndicatorHeight = 32.dp
private val IconSize = 24.dp

/**
 * Root shell.
 *
 * Android 15 (compileSdk 35) always draws edge to edge, so every screen is
 * inset by the status bar here instead of each screen guessing a top padding.
 */
@Composable
fun HumeRootScreen(settingsStore: SettingsStore, ha: HomeAssistantRepository, settings: HumeSettings) {
    if (!settings.hasToken) {
        LoginScreen(settingsStore)
        return
    }
    var tab by remember { mutableStateOf(HumeTab.Home) }

    // ContentView.swift calls ha.setActiveTab whenever the selection changes, so
    // the manager can freeze every entity the new tab does not draw.
    LaunchedEffect(tab) { ha.setActiveTab(tab) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (tab) {
                HumeTab.Energy -> EnergyScreen(ha)
                HumeTab.Security -> SecurityScreen(ha)
                HumeTab.Profile -> ProfileScreen(settingsStore, settings, ha)
                else -> HomeScreen(ha)
            }
        }

        HumeNavBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Docked bottom bar. The surface spans the full width and runs into the gesture
 * area, with only its top corners rounded, which is how One UI draws its own
 * navigation surfaces; the gesture inset is applied to the content row so the
 * background still bleeds behind the gesture bar.
 */
@Composable
private fun HumeNavBar(selected: HumeTab, onSelect: (HumeTab) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(HumeColors.Card),
    ) {
        // Hairline separator, the One UI divider above a docked bar.
        Box(Modifier.fillMaxWidth().height(1.dp).background(HumeColors.Divider))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(BarHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            navTabs.forEach { item ->
                NavItem(item = item, selected = selected == item, onClick = { onSelect(item) })
            }
        }
    }
}

/**
 * A single destination. Equal weight for every item is what keeps the row from
 * looking skewed: sizing each item by its own label length made the row drift
 * to one side.
 */
@Composable
private fun RowScope.NavItem(item: HumeTab, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val indicator by animateColorAsState(
        targetValue = if (selected) HumeColors.ChipPink else Color.Transparent,
        animationSpec = tween(180),
        label = "navIndicator",
    )
    val content by animateColorAsState(
        targetValue = if (selected) HumeColors.OrangeDeep else HumeColors.TextSecondary,
        animationSpec = tween(180),
        label = "navContent",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) IndicatorWidth else IndicatorHeight,
        animationSpec = tween(180),
        label = "navIndicatorWidth",
    )

    Column(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .width(indicatorWidth)
                .height(IndicatorHeight)
                .clip(RoundedCornerShape(50))
                .background(indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                HumeIcons.tab(item),
                contentDescription = item.label,
                tint = content,
                modifier = Modifier.size(IconSize),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            item.label,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
        )
    }
}
