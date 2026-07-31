package com.smarthome.hume.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HumeTab
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.ui.ai.AgentChatScreen
import com.smarthome.hume.ui.energy.EnergyScreen
import com.smarthome.hume.ui.home.HomeScreen
import com.smarthome.hume.ui.login.LoginScreen
import com.smarthome.hume.ui.profile.ProfileScreen
import com.smarthome.hume.ui.security.SecurityScreen
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/**
 * Root shell. The navigation is the floating design from the prototype: a round
 * accent button for Home plus a dark pill holding the remaining tabs.
 */
@Composable
fun HumeRootScreen(settingsStore: SettingsStore, ha: HomeAssistantRepository, settings: HumeSettings) {
    if (!settings.hasToken) {
        LoginScreen(settingsStore)
        return
    }
    var tab by remember { mutableStateOf(HumeTab.Home) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (tab) {
            HumeTab.Home -> HomeScreen(ha)
            HumeTab.Energy -> EnergyScreen(ha)
            HumeTab.Security -> SecurityScreen(ha)
            HumeTab.Profile -> ProfileScreen(settingsStore, settings, ha)
            HumeTab.AI -> AgentChatScreen()
        }

        FloatingNavBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
        )
    }
}

@Composable
private fun FloatingNavBar(
    selected: HumeTab,
    onSelect: (HumeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val others = HumeTab.entries.filter { it != HumeTab.Home }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (selected == HumeTab.Home) HumeColors.Orange else Color.White)
                .clickable { onSelect(HumeTab.Home) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                HumeIcons.tab(HumeTab.Home),
                contentDescription = HumeTab.Home.label,
                tint = if (selected == HumeTab.Home) Color.White else HumeColors.TextSecondary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            Modifier
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(HumeColors.Ink)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            others.forEach { item ->
                val active = selected == item
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (active) HumeColors.Orange else Color.Transparent)
                        .clickable { onSelect(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        HumeIcons.tab(item),
                        contentDescription = item.label,
                        tint = if (active) Color.White else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
