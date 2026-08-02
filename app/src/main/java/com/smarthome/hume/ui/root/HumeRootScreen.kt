package com.smarthome.hume.ui.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * Nav bar theo ban HTML cocopi-home:
 *  - thanh noi o day man hinh, cach le duoi 20dp
 *  - nut tron 56 nen Gray00, tab dang chon la vong tron 68 gradient cam
 *  - cuon xuong thi ca thanh truot xuong khoi man hinh
 */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 80.dp
private val ItemSize = 56.dp
private val ActiveSize = 68.dp

private val ActiveGradient
    get() = Brush.linearGradient(
        colors = listOf(Color(0xFFF9784C), Color(0xFFFAC0B6)),
        start = Offset(0f, 0f),
        end = Offset(180f, 200f),
    )

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
            enter = slideInVertically(animationSpec = tween(400)) { it * 2 } + fadeIn(tween(250)),
            exit = slideOutVertically(animationSpec = tween(400)) { it * 2 } + fadeOut(tween(250)),
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
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .height(BarHeight)
                .shadow(16.dp, RoundedCornerShape(BarHeight / 2), spotColor = Color.Black.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(BarHeight / 2))
                .background(HumeColors.Card)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            navTabs.forEach { item ->
                NavCircle(item = item, active = selected == item, onClick = { onSelect(item) })
            }
        }
    }
}

@Composable
private fun NavCircle(item: HumeTab, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(if (active) ActiveSize else ItemSize)
            .clip(CircleShape)
            .then(
                if (active) Modifier.background(ActiveGradient)
                else Modifier.background(HumeColors.Gray00)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            HumeIcons.tab(item),
            contentDescription = item.label,
            tint = if (active) Color.White else HumeColors.Gray500,
            modifier = Modifier.size(if (active) 30.dp else 26.dp),
        )
    }
}
