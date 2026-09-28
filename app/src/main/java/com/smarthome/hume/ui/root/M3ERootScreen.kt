package com.smarthome.hume.ui.root

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.blur
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
 * Navbar M3E: nen surfaceLowest chim 82% (+ blur tren Android 12+),
 * item duoc chon highlight TOAN O bang primaryContainer.
 */
@Composable
private fun M3ENavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.82f)
    val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(24.dp)
    } else Modifier
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .then(blurMod)
            .background(bg),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            navItems.forEachIndexed { i, item ->
                val isSel = i == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium) // 28dp
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0f),
                        )
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            item.icon, contentDescription = item.tab.label,
                            tint = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            item.tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
