package com.smarthome.hume.ui.root

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.smarthome.hume.ui.theme.glassPill

/** LiquidNavBar.swift: tabs = [.home, .energy, .security, .profile]; search is a separate button. */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 62.dp
private val SideInset = 16.dp
private val BarGap = 10.dp
private val SearchSize = 62.dp
private val IconSize = 21.dp

@Composable
fun HumeRootScreen(settingsStore: SettingsStore, ha: HomeAssistantRepository, settings: HumeSettings) {
    if (!settings.hasToken) {
        LoginScreen(settingsStore)
        return
    }
    var tab by remember { mutableStateOf(HumeTab.Home) }
    var navMinimized by remember { mutableStateOf(false) }
    LaunchedEffect(tab) { ha.setActiveTab(tab) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (tab) {
                HumeTab.Energy -> EnergyScreen(ha)
                HumeTab.Security -> SecurityScreen(ha)
                HumeTab.Profile -> ProfileScreen(settingsStore, settings, ha)
                HumeTab.AI -> AiPlaceholder()
                else -> HomeScreen(ha, onNavMinimize = { navMinimized = it })
            }
        }
        HumeNavBar(
            selected = tab,
            minimized = navMinimized && tab == HumeTab.Home,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AiPlaceholder() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "Tr\u1ee3 l\u00fd Hume AI \u0111ang \u0111\u01b0\u1ee3c ph\u00e1t tri\u1ec3n",
            fontSize = 15.sp,
            color = HumeColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HumeNavBar(
    selected: HumeTab,
    minimized: Boolean,
    onSelect: (HumeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = SideInset, end = SideInset, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(BarGap),
    ) {
        if (minimized) {
            CircleButton(active = true, onClick = { onSelect(selected) }) {
                Icon(
                    HumeIcons.tab(if (selected == HumeTab.AI) HumeTab.Home else selected),
                    contentDescription = null,
                    tint = HumeColors.Ink,
                    modifier = Modifier.size(IconSize),
                )
            }
            Spacer(Modifier.weight(1f))
        } else {
            Row(
                Modifier
                    .weight(1f)
                    .height(BarHeight)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(BarHeight / 2),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.12f),
                    )
                    .glassPill(radius = BarHeight / 2)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navTabs.forEach { item ->
                    NavItem(item = item, selected = selected == item, onClick = { onSelect(item) })
                }
            }
        }
        CircleButton(active = selected == HumeTab.AI, onClick = { onSelect(HumeTab.AI) }) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "T\u00ecm ki\u1ebfm",
                tint = HumeColors.Ink,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CircleButton(active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(SearchSize)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(SearchSize / 2),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .glassPill(radius = SearchSize / 2)
            .then(
                if (active) {
                    Modifier.background(
                        Color.White.copy(alpha = if (HumeColors.isDark) 0.16f else 0.62f),
                        RoundedCornerShape(SearchSize / 2),
                    )
                } else {
                    Modifier
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun RowScope.NavItem(item: HumeTab, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pillColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = if (HumeColors.isDark) 0.16f else 0.70f) else Color.Transparent,
        animationSpec = tween(200),
        label = "navPill",
    )
    val content by animateColorAsState(
        targetValue = if (selected) HumeColors.Ink else HumeColors.Ink.copy(alpha = 0.55f),
        animationSpec = tween(200),
        label = "navContent",
    )

    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(pillColor)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp),
        ) {
            Icon(HumeIcons.tab(item), contentDescription = item.label, tint = content, modifier = Modifier.size(IconSize))
            Spacer(Modifier.height(2.dp))
            Text(
                item.label,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = content,
                modifier = Modifier.width(64.dp),
            )
        }
    }
}
