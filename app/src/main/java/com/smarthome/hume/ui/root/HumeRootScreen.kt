package com.smarthome.hume.ui.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
 * Port tu Components/LiquidNavBar.swift.
 *
 * QUAN TRONG: PillGlass cua iOS 26 la KINH MO SANG NHE tren nen toi
 * (glassEffect(.clear) + stroke trang 0.35), KHONG PHAI mang trang duc.
 * Vi vay noi dung tab dang chon van la mau SANG (trang), khong phai mau den.
 *
 *   barHeight 66 / insetH 21 / itemW = W/4
 *   pill      (itemW - 12) x (barHeight - 14), nam gon trong thanh
 *   chon      : trang 100%, chu semibold, scale 1.0
 *   thuong    : gray1000 @ 55%, scale 0.96
 *   icon 21 / nhan 10 / spacing 3
 *   spring(damping 0.78) -> pill truot giua cac tab
 */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 66.dp
private val InsetH = 21.dp
private val PillInsetX = 12.dp
private val PillInsetY = 14.dp
private val ScrimHeight = 30.dp

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
        Box(
            Modifier
                .fillMaxWidth()
                .height(ScrimHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(background.copy(alpha = 0f), background.copy(alpha = 0.9f))
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(background.copy(alpha = 0.9f))
                .navigationBarsPadding()
                .padding(start = InsetH, end = InsetH, bottom = 14.dp, top = 2.dp),
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .shadow(18.dp, shape, spotColor = Color.Black.copy(alpha = 0.55f))
                    .clip(shape)
                    .background(HumeColors.Card)
                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            ) {
                val itemWidth: Dp = maxWidth / navTabs.size
                val pillWidth: Dp = itemWidth - PillInsetX
                val pillHeight: Dp = BarHeight - PillInsetY
                val activeIndex = navTabs.indexOf(selected).coerceAtLeast(0)
                val pillX by animateDpAsState(
                    targetValue = itemWidth * activeIndex + PillInsetX / 2,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "pillX",
                )

                // PillGlass: kinh mo SANG NHE (khong phai trang duc, khong mau cam).
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = pillX)
                        .width(pillWidth)
                        .height(pillHeight)
                        .clip(RoundedCornerShape(pillHeight / 2))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.10f),
                                )
                            )
                        )
                        .border(
                            0.5.dp,
                            Color.White.copy(alpha = 0.35f),
                            RoundedCornerShape(pillHeight / 2),
                        )
                )

                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    navTabs.forEach { item ->
                        NavItem(
                            item = item,
                            active = selected == item,
                            onClick = { onSelect(item) },
                            modifier = Modifier.width(itemWidth).fillMaxHeight(),
                        )
                    }
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
    val contentColor by animateColorAsState(
        if (active) HumeColors.Gray1000 else HumeColors.Gray1000.copy(alpha = 0.55f),
        tween(220),
        label = "navContent",
    )
    val scale by animateFloatAsState(if (active) 1f else 0.96f, tween(220), label = "navScale")

    Column(
        modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            HumeIcons.tab(item),
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(21.dp),
        )
        Text(
            item.label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
