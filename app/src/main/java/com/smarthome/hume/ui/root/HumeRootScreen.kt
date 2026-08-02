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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
 * Navbar One UI - doi chieu 2 anh chup thanh nav app Dien thoai (light + dark).
 *
 * NHUNG DIEU ANH GOC CHO THAY:
 *  1. Thanh KHONG keo het be ngang. No la mot vien thuoc NGAN, chi rong bang
 *     tong cac o tab, va CAN GIUA - hai ben van thay noi dung phia sau.
 *  2. KHONG co blur. Nen chi la mot bac xam so voi nen man hinh:
 *     dark -> #2B2B2B tren nen den, light -> #F2F2F2 tren nen trang.
 *  3. Pill tab chon chi lech mot bac nua: dark #3C3C3C, light #E0E0E0. Khong vien.
 *  4. Chu tab chon dam va sang; tab thuong xam nhat.
 *
 * => bar width = itemWidth * so tab + inset*2, wrapContentWidth + canh giua.
 */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 56.dp
private val BarInset = 6.dp
private val ItemWidth = 76.dp
private val PillHeight = 44.dp

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
    val dark = HumeColors.isDark
    val barColor = if (dark) Color(0xFF2B2B2B) else Color(0xFFF2F2F2)
    val barEdge = if (dark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)
    val pillColor = if (dark) Color(0xFF3C3C3C) else Color(0xFFE0E0E0)
    val activeIndex = navTabs.indexOf(selected).coerceAtLeast(0)
    val pillX by animateDpAsState(
        targetValue = BarInset + ItemWidth * activeIndex,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "pillX",
    )

    Box(
        Modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            // Thanh NGAN: chi rong bang tong cac o tab, khong fillMaxWidth.
            .width(ItemWidth * navTabs.size + BarInset * 2)
            .height(BarHeight)
            .shadow(10.dp, shape, spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(shape)
            .background(barColor)
            .border(0.5.dp, barEdge, shape),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = pillX)
                .width(ItemWidth)
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
                    modifier = Modifier.width(ItemWidth).fillMaxHeight(),
                )
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
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
