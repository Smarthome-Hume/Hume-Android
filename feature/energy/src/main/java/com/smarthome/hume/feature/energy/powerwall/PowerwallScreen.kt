package com.smarthome.hume.feature.energy.powerwall

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.ui.components.M3EHeaderFade
import org.json.JSONObject

/**
 * Trang mo phong Powerwall 3D — port 1:1 iOS `PowerwallSimulationView.swift`.
 *
 * - WebView full man load bundle tinh `powerwall-dashboard/index.html`
 *   (Three.js r186 inline, chay hoan toan offline). Nen trang la nen HTML
 *   cua webview (surface qua themeVars); lop surface cua Box ben duoi chi
 *   la nen cho luc WebView chua ve xong (giong iOS dat
 *   webView.backgroundColor = pageBackground) — iOS v60 cam lop surface
 *   full man O TREN webview lam vung header co nen dong mau trang, khong
 *   cam nen duoi webview.
 * - Live data day qua `window.__humePowerwallLive`, theme qua
 *   `window.__humePowerwallTheme` (giong iOS Coordinator.pushIfReady).
 * - An the pack trong trang -> sheet chi tiet native; nut setting goc the
 *   he thong -> sheet setting native (qua `window.HumeAndroid`, xem
 *   [PowerwallBridge]; bundle iOS dung `window.webkit.messageHandlers.hume`).
 * - Pill title "Powerwall" la thu duy nhat bam duoc de ve (iOS v53: dat
 *   onTapGesture ca HStack full-width se nuot tap vao pack Master o nua
 *   tren 3D). Vuot back he thong do caller (N-05) xu ly.
 *
 * KHAC BIET SO VOI iOS (ghi ro, khong giau):
 * - iOS chen theme truoc lan ve dau bang WKUserScript (document-start).
 *   Android WebView khong co document-start injection: lan ve dau dung
 *   mau mac dinh toi cua bundle, theme day ngay sau onPageFinished roi
 *   day lai moi khi doi seed/sang-toi (cung duong day nhu iOS updateUIView).
 * - iOS day noi dung webview xuong 130pt bang scrollView.contentInset.top.
 *   Android WebView khong co contentInset: tuong duong bang cach dat
 *   `.wrap` padding-top = 148px (130 + 18px goc) sau khi trang load xong
 *   (xem [CONTENT_TOP_INSET_PX]).
 * - iOS vuot tu mep trai (>80pt) de ve Home (simultaneousGesture). Android
 *   dung system back gesture san co — khong them gesture rieng de tranh
 *   nham voi xoay 3D.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PowerwallScreen(
    onBack: () -> Unit,
    vm: PowerwallViewModel = viewModel(factory = PowerwallViewModel.factory()),
) {
    val cs = MaterialTheme.colorScheme
    val extra = com.smarthome.hume.core.ui.theme.LocalHumeExtraColors.current
    val payload by vm.payload.collectAsState()
    val payloadJson by vm.payloadJson.collectAsState()

    // Theme vars nhu iOS themeVars(colors:isDark:). Day lai moi khi doi seed /
    // sang-toi (body observe truc tiep — iOS: tinh themeVars NGOAI
    // GroupEntityReader de observation toi duoc view ngoai).
    val themeVars = remember(cs, extra) { PowerwallLiveData.themeVars(cs, extra) }
    val themeJson = remember(themeVars) { JSONObject(themeVars).toString() }

    var packSheetKey by remember { mutableStateOf<String?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }

    // Trang thai day du lieu (port iOS Coordinator: isReady + pending).
    val bridge = remember {
        PowerwallBridge(
            onPackTap = { key -> packSheetKey = key },
            onSettingsTap = { settingsOpen = true },
        )
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // iOS fade height 130 = screenTopInset (~59pt) + 71pt phan header.
    // Android: status bar that + 71dp.
    val fadeHeight = statusBarTop + 71.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(cs.surface),
    ) {
        // Webview full man ben duoi.
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Bundle la file don trong assets: can file access
                    // de load file:///android_asset/... (mac dinh bat).
                    settings.allowFileAccess = true
                    settings.setSupportZoom(false)
                    // Nen webview theo theme (tranh flash trang luc load).
                    setBackgroundColor(cs.surface.toArgb())
                    addJavascriptInterface(bridge, "HumeAndroid")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            bridge.markReady(view)
                            // Day noi dung xuong duoi title o trang thai
                            // cao nhat (iOS v61: contentInset 130).
                            view.evaluateJavascript(
                                "document.querySelector('.wrap').style.paddingTop='${CONTENT_TOP_INSET_PX}px';",
                                null,
                            )
                            bridge.pushTheme(themeJson)
                            bridge.pushData(payloadJson)
                        }
                    }
                    loadUrl("file:///android_asset/$POWERWALL_ASSET_PATH")
                }
            },
            update = { webView ->
                // Theme day ngay (hiem); data chi day khi payload doi that
                // (ViewModel da throttle 2s + distinct — nhu iOS updateUIView).
                webView.setBackgroundColor(cs.surface.toArgb())
                bridge.pushTheme(themeJson)
                bridge.pushData(payloadJson)
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
            modifier = Modifier.fillMaxSize(),
        )
        // v63/v74: lop fade gradient kieu Zalo — lop RIENG giua webview va
        // title: dac 0.95 o tren, gradient 72dp mo dan xuong trong suot;
        // title nam TREN fade, luon ro net. Khong chan cham.
        M3EHeaderFade(
            color = cs.surface,
            height = fadeHeight,
            modifier = Modifier.align(Alignment.TopStart),
        )
        // Title co dinh phu tren — KHONG nen mau dac, KHONG material
        // phang; chi pill title co nen rieng cua no. Noi dung webview
        // cuon ben duoi, tan dan qua lop fade.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = statusBarTop + 12.dp, bottom = 6.dp),
        ) {
            // v48: title boc nen Y HET cac trang khac (Nang luong / An ninh /
            // Ho so): pill primaryContainer + chu onPrimaryContainer, bo 28,
            // tracking -0.3, padding 20/12 — theo theme (doi seed/sang-toi
            // la pill doi theo). Chi pill moi bam duoc de ve (v53).
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(cs.primaryContainer)
                    .clickable { onBack() }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    "Powerwall",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    color = cs.onPrimaryContainer,
                )
            }
        }
    }

    // v57: sheet pack chuan nhu cac sheet khac (detent 0.85, bo 60, nen
    // surfaceContainer). Dat o day (ngoai WebView) de sheet khong bi render
    // lai theo tung lan payload doi; sheet tu doc live data ben trong.
    packSheetKey?.let { key ->
        PowerwallPackSheet(
            packKey = key,
            payload = payload,
            onDismiss = { packSheetKey = null },
        )
    }
    // v65/v73: sheet setting native — detent theo chieu cao noi dung do that
    // (xem PowerwallSettingsSheet), bo 60, nen surfaceContainer.
    if (settingsOpen) {
        PowerwallSettingsSheet(
            payload = payload,
            onDismiss = { settingsOpen = false },
        )
    }
}

/** Duong dan bundle trong assets (module :feature:energy, merge vao APK). */
private const val POWERWALL_ASSET_PATH = "powerwall-dashboard/index.html"

/**
 * Tuong duong iOS contentInset.top = 130: `.wrap` trong bundle von co
 * padding-top 18px; dat 148px de the 3D dau tien nam duoi pill title o
 * trang thai cao nhat. Noi dung van cuon len duoi title khi user keo
 * (chi doi vi tri nghi o dau trang).
 */
private const val CONTENT_TOP_INSET_PX = 148

/**
 * Cau noi JS -> native (port iOS WKScriptMessageHandler "hume").
 * Trang goi `window.HumeAndroid.postMessage(JSON-string)` (shim `humePost`
 * trong bundle); iOS goi `window.webkit.messageHandlers.hume.postMessage`.
 * Tin chay tren thread JS bridge -> post ve main truoc khi doi state.
 *
 * v52: {type:"pack", key} -> mo sheet pack. v65: {type:"settings"} -> mo
 * sheet setting native (khong mo modal HTML nua).
 */
private class PowerwallBridge(
    private val onPackTap: (String) -> Unit,
    private val onSettingsTap: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var isReady = false
    private var lastThemeJson: String? = null
    private var lastDataJson: String? = null
    private var pendingThemeJson: String? = null
    private var pendingDataJson: String? = null

    fun markReady(view: WebView) {
        webView = view
        isReady = true
        pushPending()
    }

    /** Day theme (chi khi doi that, nhu iOS updateUIView). */
    fun pushTheme(themeJson: String) {
        if (themeJson == lastThemeJson) return
        lastThemeJson = themeJson
        pendingThemeJson = themeJson
        pushPending()
    }

    /** Day data (chi khi doi that; ViewModel da throttle 2s). */
    fun pushData(dataJson: String?) {
        if (dataJson == null || dataJson == lastDataJson) return
        lastDataJson = dataJson
        pendingDataJson = dataJson
        pushPending()
    }

    private fun pushPending() {
        val view = webView ?: return
        if (!isReady) return
        pendingThemeJson?.let { json ->
            pendingThemeJson = null
            view.evaluateJavascript(
                "window.__humePowerwallTheme && window.__humePowerwallTheme($json)",
                null,
            )
        }
        pendingDataJson?.let { json ->
            pendingDataJson = null
            view.evaluateJavascript(
                "window.__humePowerwallLive && window.__humePowerwallLive($json)",
                null,
            )
        }
    }

    @JavascriptInterface
    fun postMessage(json: String) {
        val type: String?
        val key: String?
        try {
            val obj = JSONObject(json)
            type = obj.optString("type").takeIf { it.isNotEmpty() }
            key = obj.optString("key").takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            return
        }
        mainHandler.post {
            when (type) {
                "pack" -> key?.let(onPackTap)
                "settings" -> onSettingsTap()
            }
        }
    }
}
