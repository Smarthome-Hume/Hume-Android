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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
 *  1. Thanh KHONG keo het be ngang: vien thuoc NGAN, rong bang tong cac o tab.
 *  2. KHONG blur.
 *  3. NEN THANH = DUNG mau nen cua the phong (HumeColors.Card), tuc #161616 o
 *     nen toi va #FFFFFF o nen sang. Khong dung mau xam rieng.
 *  4. Pill tab chon lech mot bac so voi nen thanh, khong vien.
 *  5. NET ICON: tab khong chon dung CUNG mau net voi tab dang chon. Phan biet
 *     tab dang chon bang NEN PILL + icon dac, khong bang cach lam mo net icon.
 *  6. Be rong o tab CHIA DEU theo be rong thuc te (weight), chi bi chan tren
 *     boi MaxItemWidth. Man hep (Multi-window, Pop-up view, Fold man ngoai)
 *     khong con tran ngang.
 */
private val navTabs = listOf(HumeTab.Home, HumeTab.Energy, HumeTab.Security, HumeTab.Profile)
private val BarHeight = 56.dp
private val BarInset = 6.dp
private val MaxItemWidth = 76.dp
private val PillHeight = 44.dp

@Composable
fun HumeRootScreen(settingsStore: SettingsStore, ha: HomeAssistantRepository, settings: HumeSettings) {
    if (!settings.hasToken) {
        LoginScreen(settingsStore)
        return
    }
    // rememberSaveable: One UI doi uiMode (che do toi theo lich), co chu, xoay
    // may, DeX va Multi-window rat thuong xuyen. Khong duoc mat tab dang xem.
    var tab by rememberSaveable { mutableStateOf(HumeTab.Home) }
    var navHidden by rememberSaveable { mutableStateOf(false) }
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
    // Nen thanh = nen the phong.
    val barColor = HumeColors.Card
    val barEdge = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f)
    // Pill lech mot bac so voi nen thanh.
    val pillColor = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.07f)
    val activeIndex = navTabs.indexOf(selected).coerceAtLeast(0)

    BoxWithConstraints(
        Modifier
            // navigationBarsPadding: Samsung cho chon thanh 3 nut (~48dp) hoac
            // cu chi (~24dp), nen khong duoc dung khoang chua cung.
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .widthIn(max = MaxItemWidth * navTabs.size + BarInset * 2)
            .fillMaxWidth()
            .height(BarHeight)
            .shadow(10.dp, shape, spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(shape)
            .background(barColor)
            .border(0.5.dp, barEdge, shape),
    ) {
        val itemWidth = (maxWidth - BarInset * 2) / navTabs.size
        val pillX by animateDpAsState(
            targetValue = BarInset + itemWidth * activeIndex,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
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
                    modifier = Modifier.weight(1f).fillMaxHeight(),
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
    // Net icon tab KHONG chon dung cung mau voi tab dang chon (trang tren nen
    // toi), chi khac o do day nhan va nen pill.
    val contentColor by animateColorAsState(
        HumeColors.Gray1000,
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
