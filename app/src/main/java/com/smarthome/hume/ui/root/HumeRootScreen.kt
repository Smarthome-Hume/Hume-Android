package com.smarthome.hume.ui.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

/*
 * Navbar One UI 8.5 - floating tab bar.
 *  - MOT pill lien khoi, NEN DAC (Compose khong blur duoc nen backdrop; de trong suot
 *    thi noi dung phia sau loi qua nhin rat ban -> dung mau dac + scrim mo dan phia tren).
 *  - Le trai/phai 14dp, le duoi 14dp, cao 58dp, bo tron nua chieu cao.
 *  - 4 tab chia deu, CHI ICON, tab chon = capsule highlighter cung co.
 */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 58.dp
private val BarSideMargin = 14.dp
private val BarBottomMargin = 14.dp
private val CapsuleHeight = 40.dp
private val CapsuleWidth = 58.dp
private val ScrimHeight = 28.dp

@Composable
fun HumeRootScreen(settingsStore: SettingsStore, ha: HomeAssistantRepository, settings: HumeSettings) {
    if (!settings.hasToken) {
        LoginScreen(settingsStore)
        return
    }
    var tab by remember { mutableStateOf(HumeTab.Home) }
    var navHidden by remember { mutableStateOf(false) }
    LaunchedEffect(tab) { ha.setActiveTab(tab) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (tab) {
                HumeTab.Energy -> EnergyScreen(ha)
                HumeTab.Security -> SecurityScreen(ha)
                HumeTab.Profile -> ProfileScreen(settingsStore, settings, ha)
                HumeTab.AI -> AiPlaceholder()
                else -> HomeScreen(ha, onNavMinimize = { navHidden = it })
            }
        }
        AnimatedVisibility(
            visible = !(navHidden && tab == HumeTab.Home),
            enter = slideInVertically(animationSpec = tween(360)) { it * 2 } + fadeIn(tween(200)),
            exit = slideOutVertically(animationSpec = tween(360)) { it * 2 } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            HumeNavBar(selected = tab, onSelect = { tab = it })
        }
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
private fun HumeNavBar(selected: HumeTab, onSelect: (HumeTab) -> Unit) {
    val shape = RoundedCornerShape(BarHeight / 2)
    val background = MaterialTheme.colorScheme.background
    Column(Modifier.fillMaxWidth()) {
        // Scrim: noi dung cuon toi day mo dan vao nen, khong dam sam vao thanh nav.
        Box(
            Modifier
                .fillMaxWidth()
                .height(ScrimHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(background.copy(alpha = 0f), background.copy(alpha = 0.85f))
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(background.copy(alpha = 0.85f))
                .navigationBarsPadding()
                .padding(start = BarSideMargin, end = BarSideMargin, bottom = BarBottomMargin, top = 2.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .shadow(12.dp, shape, spotColor = Color.Black.copy(alpha = 0.6f))
                    .clip(shape)
                    .background(HumeColors.Card)
                    .border(1.dp, HumeColors.Gray1000.copy(alpha = 0.07f), shape),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navTabs.forEach { item ->
                    NavItem(
                        item = item,
                        active = selected == item,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    item: HumeTab,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(140), label = "navPress")
    val capsuleColor by animateColorAsState(
        if (active) HumeColors.Orange.copy(alpha = 0.16f) else Color.Transparent,
        tween(240),
        label = "navCapsule",
    )
    val iconColor by animateColorAsState(
        if (active) HumeColors.Orange else HumeColors.Gray500,
        tween(240),
        label = "navIcon",
    )
    val capsuleW by animateDpAsState(if (active) CapsuleWidth else 0.dp, tween(260), label = "navCapsuleW")

    Box(
        modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = capsuleW, height = CapsuleHeight)
                .clip(RoundedCornerShape(CapsuleHeight / 2))
                .background(capsuleColor)
        )
        Icon(
            HumeIcons.tab(item),
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(23.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        )
    }
}
