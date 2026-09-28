package com.smarthome.hume.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HumeTab
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.core.ui.theme.HumeM3ETheme
import com.smarthome.hume.core.ui.theme.M3ESeed
import com.smarthome.hume.feature.home.HomeScreen
import com.smarthome.hume.feature.energy.EnergyScreen as M3EEnergyScreen
import com.smarthome.hume.feature.me.MeScreen
import com.smarthome.hume.feature.security.SecurityScreen as M3ESecurityScreen

private data class NavItem(val tab: HumeTab, val icon: ImageVector)

private val navItems = listOf(
    NavItem(HumeTab.Home, Icons.Outlined.Home),
    NavItem(HumeTab.Energy, Icons.Outlined.Bolt),
    NavItem(HumeTab.Security, Icons.Outlined.Shield),
    NavItem(HumeTab.Profile, Icons.Outlined.Person),
)

/**
 * Root M3E: theme moi + navbar highlight full-item primaryContainer.
 * Tab Nha dung feature:home moi; cac tab con lai tam dung man hinh cu
 * (se port dan o cac cum tiep theo).
 */
@Composable
fun M3ERootScreen(
    settingsStore: SettingsStore,
    ha: HomeAssistantRepository,
    settings: HumeSettings,
) {
    val themeSettings by HumeGraph.get().themeStore.settings.collectAsState(
        initial = com.smarthome.hume.core.datastore.ThemeSettings(),
    )
    val seed = runCatching { M3ESeed.valueOf(themeSettings.seedName) }.getOrDefault(M3ESeed.Cam)
    val darkTheme = themeSettings.darkMode ?: isSystemInDarkTheme()
    HumeM3ETheme(seed = seed, darkTheme = darkTheme) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        Scaffold(
            bottomBar = {
                M3ENavBar(
                    selected = selected,
                    onSelect = { selected = it },
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
            ) {
                when (navItems[selected].tab) {
                    HumeTab.Home -> HomeScreen()
                    HumeTab.Energy -> M3EEnergyScreen()
                    HumeTab.Security -> M3ESecurityScreen()
                    HumeTab.Profile -> MeScreen(onViewCamera = { selected = 2 })
                }
            }
        }
    }
}

/**
 * Navbar M3E theo demo v4 (.nav/.navit): FLOATING — cach 2 canh 16dp,
 * cach day 20dp, bo 34dp, nen surfaceLowest 82% + shadow;
 * item chon highlight TOAN O primaryContainer (khong pill tach roi),
 * icon outlined, label dam khi chon.
 */
@Composable
private fun M3ENavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(34.dp)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .shadow(8.dp, pill)
                .clip(pill)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.82f))
                .padding(10.dp),
        ) {
            navItems.forEachIndexed { i, item ->
                val isSel = i == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0f),
                        )
                        .clickable { onSelect(i) }
                        .padding(vertical = 9.dp),
                ) {
                    Icon(
                        item.icon, contentDescription = item.tab.label,
                        tint = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isSel) 27.dp else 24.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
