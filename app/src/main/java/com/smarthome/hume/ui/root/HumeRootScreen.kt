package com.smarthome.hume.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
 * Root shell.
 *
 * Android 15 (compileSdk 35) always draws edge to edge, so every screen is
 * inset by the status bar here instead of each screen guessing a top padding.
 * The navigation bar is a floating One UI pill: opaque so scrolling content
 * never bleeds through it, lifted above the gesture bar.
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

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(HumeColors.Card)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            navTabs.forEach { item ->
                NavItem(item = item, selected = tab == item, onClick = { tab = item })
            }
        }
    }
}

@Composable
private fun NavItem(item: HumeTab, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) HumeColors.ChipPink else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            HumeIcons.tab(item),
            contentDescription = item.label,
            tint = if (selected) HumeColors.OrangeDeep else HumeColors.TextSecondary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            item.label,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) HumeColors.OrangeDeep else HumeColors.TextSecondary,
        )
    }
}
