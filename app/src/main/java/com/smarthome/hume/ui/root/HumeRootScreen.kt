package com.smarthome.hume.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.smarthome.hume.ui.theme.HumeSpacing
import com.smarthome.hume.ui.theme.glassPill

/** The only tabs the app ships with. The AI butler tab was removed. */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)

/**
 * Root shell. The navigation bar is a floating glass pill, which is how One UI
 * 8.5 renders in app toolbars: lifted off the bottom edge, never docked.
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
        when (tab) {
            HumeTab.Energy -> EnergyScreen(ha)
            HumeTab.Security -> SecurityScreen(ha)
            HumeTab.Profile -> ProfileScreen(settingsStore, settings, ha)
            else -> HomeScreen(ha)
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = HumeSpacing.Large)
                .glassPill()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navTabs.forEach { item ->
                NavItem(item = item, selected = tab == item, onClick = { tab = item })
            }
        }
    }
}

@Composable
private fun NavItem(item: HumeTab, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) HumeColors.ChipPink else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            HumeIcons.tab(item),
            contentDescription = item.label,
            tint = if (selected) HumeColors.OrangeDeep else HumeColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            item.label,
            fontSize = 9.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) HumeColors.OrangeDeep else HumeColors.TextSecondary,
        )
    }
}
