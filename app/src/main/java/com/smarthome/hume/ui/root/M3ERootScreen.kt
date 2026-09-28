package com.smarthome.hume.ui.root

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HumeTab
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.rememberNeighborPress
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.HumeM3ETheme
import com.smarthome.hume.core.ui.theme.M3ESeed
import com.smarthome.hume.feature.home.HomeScreen
import com.smarthome.hume.feature.energy.EnergyScreen as M3EEnergyScreen
import com.smarthome.hume.feature.me.MeScreen
import com.smarthome.hume.feature.security.SecurityScreen as M3ESecurityScreen

private data class NavItem(val tab: HumeTab, val icon: String)

private val navItems = listOf(
    NavItem(HumeTab.Home, Ms.home),
    NavItem(HumeTab.Energy, Ms.bolt),
    NavItem(HumeTab.Security, Ms.shield),
    NavItem(HumeTab.Profile, Ms.person),
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
        // Navbar NOI tren be mat trang: dung Box overlay thay vi Scaffold bottomBar
        // (Scaffold bottomBar van giu cho layout). Content full-bleed, navbar noi phia tren.
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(Modifier.fillMaxSize()) {
                when (navItems[selected].tab) {
                    HumeTab.Home -> HomeScreen()
                    HumeTab.Energy -> M3EEnergyScreen()
                    HumeTab.Security -> M3ESecurityScreen()
                    HumeTab.Profile -> MeScreen(onViewCamera = { selected = 2 })
                }
            }
            M3ENavBar(
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Navbar M3E theo demo v4 (.nav/.navit): FLOATING — cach 2 canh 16dp,
 * cach day 20dp, bo 34dp, nen surfaceLowest 98% + shadow (gan nhu dac,
 * M3E khong co thiet ke trong suot);
 * item chon highlight TOAN O primaryContainer (khong pill tach roi),
 * icon outlined (scale 1.12 khi chon), label dam khi chon;
 * neighbor-press: item dang nhan no rong (spring), 2 item ke co lai;
 * :active nen surfaceContainer. Backdrop blur bo qua (ghi nhan gioi han).
 */
@Composable
private fun M3ENavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val pill = RoundedCornerShape(34.dp)
    val np = rememberNeighborPress(navItems.size, 1.18f, 0.93f)
    val haptic = rememberHaptic()
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .shadow(12.dp, pill)
                .clip(pill)
                .background(extra.surfaceLowest.copy(alpha = 0.98f))
                .padding(10.dp),
        ) {
            navItems.forEachIndexed { i, item ->
                val isSel = i == selected
                val pressed = np.pressedIndex == i
                // Selected flex-grow + neighbor shrink, mo phong .navit flex-grow transition
                val weight by animateFloatAsState(
                    targetValue = np.weightFor(i),
                    animationSpec = tween(500, easing = M3EMotion.spring),
                    label = "navW$i",
                )
                val iconSize by animateDpAsState(
                    targetValue = if (isSel) 27.dp else 24.dp,
                    animationSpec = tween(300, easing = M3EMotion.spring),
                    label = "navI$i",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(weight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            when {
                                pressed -> cs.surfaceContainer
                                isSel -> cs.primaryContainer
                                else -> Color.Transparent
                            },
                        )
                        .pointerInput(i) {
                            detectTapGestures(
                                onPress = {
                                    haptic()
                                    np.press(i)
                                    tryAwaitRelease()
                                    np.release()
                                },
                                onTap = { onSelect(i) },
                            )
                        }
                        .padding(top = 9.dp, bottom = 7.dp),
                ) {
                    // Active: icon fill dac (Canvas Path); inactive: outlined
                    if (isSel) {
                        FilledNavIcon(
                            tab = item.tab,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(iconSize),
                        )
                    } else {
                        MsIcon(
                            item.icon, contentDescription = item.tab.label,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSel) cs.onPrimaryContainer
                        else cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
