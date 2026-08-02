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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
 * Navbar theo dung nguyen tac cua iOS 26 Liquid Glass tab bar / One UI 8.5 floating tab bar:
 *
 *  1. MOT pill lien khoi noi tren noi dung, le trai/phai/duoi bang nhau (16dp).
 *  2. Be ngang pill chia thanh N O BANG NHAU tuyet doi (itemWidth = (W - 2*inset)/N).
 *     Icon nam chinh giua o cua no -> khoang cach giua cac icon luon deu.
 *  3. Tab dang chon co LOP NEN DANG VIEN THUOC (capsule) nam duoi icon, cung kich thuoc
 *     cho moi tab, va TRUOT (spring) tu o cu sang o moi khi doi tab - day la dac trung
 *     cua ca hai he dieu hanh, khong phai vong tron phong to.
 *  4. Chi icon, khong nhan chu (One UI 8.5 da bo nhan).
 *  5. Nen pill DAC + scrim mo dan phia tren (Compose khong blur duoc backdrop that).
 */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 64.dp
private val BarMargin = 16.dp
private val BarInset = 6.dp
private val CapsuleHeight = 48.dp
private val CapsuleMaxWidth = 68.dp
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
                .padding(start = BarMargin, end = BarMargin, bottom = BarMargin, top = 2.dp),
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .shadow(14.dp, shape, spotColor = Color.Black.copy(alpha = 0.6f))
                    .clip(shape)
                    .background(HumeColors.Card)
                    .border(1.dp, HumeColors.Gray1000.copy(alpha = 0.08f), shape)
            ) {
                // Chia o bang nhau tuyet doi -> khoang cach giua cac icon deu nhau.
                val itemWidth: Dp = (maxWidth - BarInset * 2) / navTabs.size
                val capsuleWidth: Dp = minOf(itemWidth - 6.dp, CapsuleMaxWidth)
                val activeIndex = navTabs.indexOf(selected).coerceAtLeast(0)
                val capsuleX by animateDpAsState(
                    targetValue = BarInset + itemWidth * activeIndex + (itemWidth - capsuleWidth) / 2,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "capsuleX",
                )

                // Lop nen dang vien thuoc cua tab dang chon, truot giua cac o.
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = capsuleX)
                        .width(capsuleWidth)
                        .height(CapsuleHeight)
                        .clip(RoundedCornerShape(CapsuleHeight / 2))
                        .background(HumeColors.Orange.copy(alpha = 0.22f))
                        .border(
                            1.dp,
                            HumeColors.Orange.copy(alpha = 0.35f),
                            RoundedCornerShape(CapsuleHeight / 2),
                        )
                )

                Row(
                    Modifier.fillMaxSize().padding(horizontal = BarInset),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.88f else 1f, tween(140), label = "navPress")
    val iconColor by animateColorAsState(
        if (active) HumeColors.Orange else HumeColors.Gray500,
        tween(240),
        label = "navIcon",
    )

    Box(
        modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            HumeIcons.tab(item),
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { scaleX = press; scaleY = press },
        )
    }
}
