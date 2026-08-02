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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/*
 * Navbar theo dung floating tab bar cua One UI (doi chieu anh chup thanh nav app Dien thoai).
 *
 *  - Nen thanh: BAM THEO THEME. Toi -> xam #2B2B2B, sang -> #F7F7F7 (khong phai
 *    tint den de len nen sang -> tranh mang xam ban nhu ban truoc). Van blur nen that.
 *  - Pill tab chon: mang XAM MEM, KHONG VIEN, khong gradient trang, chi sang/toi hon
 *    nen thanh mot bac (Gray00) -> hoa vao thanh dung kieu One UI.
 *  - Mau chu/icon: chon -> Gray1000 (trang o dark, den o light), thuong -> Gray500.
 *  - Ti le: thanh 60, pill 44 (= thanh - 16), inset 8, le ngoai 16, icon 22, nhan 11.
 */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 60.dp
private val BarSideMargin = 16.dp
private val BarInset = 8.dp
private val PillHeight = 44.dp

@Composable
fun HumeRootScreen(settingsStore: SettingsStore, ha: HomeAssistantRepository, settings: HumeSettings) {
    if (!settings.hasToken) {
        LoginScreen(settingsStore)
        return
    }
    var tab by remember { mutableStateOf(HumeTab.Home) }
    var navHidden by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    LaunchedEffect(tab) { ha.setActiveTab(tab) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .statusBarsPadding()
        ) {
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
            HumeNavBar(selected = tab, hazeState = hazeState, onSelect = { tab = it })
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
private fun HumeNavBar(selected: HumeTab, hazeState: HazeState, onSelect: (HumeTab) -> Unit) {
    val shape = RoundedCornerShape(BarHeight / 2)
    val dark = HumeColors.isDark
    // Mau nen thanh lay dung tinh than One UI: mot bac xam so voi nen trang/den.
    val barTint = if (dark) Color(0xFF2B2B2B).copy(alpha = 0.86f) else Color(0xFFF7F7F7).copy(alpha = 0.88f)
    // Pill chi sang/toi hon thanh MOT bac, khong vien, khong gradient.
    val pillColor = if (dark) Color(0xFF3C3C3C) else Color(0xFFE3E3E3)

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = BarSideMargin, end = BarSideMargin, bottom = 12.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .shadow(12.dp, shape, spotColor = Color.Black.copy(alpha = 0.45f))
                .clip(shape)
                .hazeEffect(state = hazeState) {
                    blurRadius = 28.dp
                    backgroundColor = MaterialTheme.colorScheme.background
                    tints = listOf(HazeTint(barTint))
                    noiseFactor = 0f
                }
        ) {
            val itemWidth: Dp = (maxWidth - BarInset * 2) / navTabs.size
            val activeIndex = navTabs.indexOf(selected).coerceAtLeast(0)
            val pillX by animateDpAsState(
                targetValue = BarInset + itemWidth * activeIndex,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "pillX",
            )

            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = pillX)
                    .width(itemWidth)
                    .height(PillHeight)
                    .clip(RoundedCornerShape(PillHeight / 2))
                    .background(pillColor)
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

@Composable
private fun NavItem(
    item: HumeTab,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        if (active) HumeColors.Gray1000 else HumeColors.Gray500,
        tween(220),
        label = "navContent",
    )
    val scale by animateFloatAsState(if (active) 1f else 0.97f, tween(220), label = "navScale")

    Column(
        modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            HumeIcons.tab(item, active),
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            item.label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
