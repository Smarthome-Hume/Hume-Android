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
 * Navbar theo One UI 8.5 "floating tab bar":
 *  - MOT thanh pill lien khoi, bo tron hoan toan, co le trai/phai 12dp va le duoi 12dp
 *  - nen frosted: mau surface ban trong suot + vien sang mo, do bong nhe -> noi tren noi dung
 *  - 4 tab chia deu chieu ngang, CHI ICON (One UI 8.5 da bo nhan chu duoi icon)
 *  - tab dang chon: highlighter dang capsule nam SAU icon, cung co voi cac tab khac (khong phong to)
 *  - cuon xuong: ca thanh truot xuong khoi man hinh
 */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 60.dp
private val BarSideMargin = 12.dp
private val CapsuleHeight = 44.dp
private val CapsuleWidth = 62.dp

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
            enter = slideInVertically(animationSpec = tween(380)) { it * 2 } + fadeIn(tween(220)),
            exit = slideOutVertically(animationSpec = tween(380)) { it * 2 } + fadeOut(tween(220)),
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
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = BarSideMargin, end = BarSideMargin, bottom = 12.dp)
            .height(BarHeight)
            .shadow(14.dp, shape, spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(shape)
            .background(HumeColors.Card.copy(alpha = 0.82f))
            .border(1.dp, HumeColors.Gray1000.copy(alpha = 0.06f), shape),
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
        if (active) HumeColors.Orange.copy(alpha = 0.18f) else Color.Transparent,
        tween(260),
        label = "navCapsule",
    )
    val iconColor by animateColorAsState(
        if (active) HumeColors.Orange else HumeColors.Gray500,
        tween(260),
        label = "navIcon",
    )
    val capsuleW by animateDpAsState(if (active) CapsuleWidth else 0.dp, tween(280), label = "navCapsuleW")

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
            modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        )
    }
}
