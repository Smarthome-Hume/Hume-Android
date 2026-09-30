package com.smarthome.hume.brief

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.View
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.ui.components.M3EMotion

/**
 * Host cua trang Brief o tang root:
 * - Cua vuot canh trai (32dp) de MO khi dang dong.
 * - Trang Brief truot tu trai vao (emphasized), vuot phai->trai bat ky dau tren trang de DONG.
 * - Nut back / system back gesture khi mo -> dong trang (khong thoat app).
 * - Gesture exclusion rect o canh trai khi dong de vuot canh cua user khong bi
 *   system back gesture cuop.
 */
@Composable
fun BriefEdgeHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vm: BriefViewModel = rememberBriefViewModel()
    val view = LocalView.current
    val density = LocalDensity.current
    var open by rememberSaveable { mutableStateOf(false) }

    val cache by vm.cache.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()

    // Back (nut + gesture) khi trang mo -> dong trang.
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(open, dispatcher) {
        val cb = object : androidx.activity.OnBackPressedCallback(open) {
            override fun handleOnBackPressed() { open = false }
        }
        dispatcher?.addCallback(cb)
        onDispose { cb.remove() }
    }

    // Exclusion rect canh trai khi dong: vuot canh thuoc ve app, khong phai system back.
    // (2026-09-30, fix vuot canh khong mo duoc Brief) Ban cu dat rect trong
    // LaunchedEffect(open) ngay sau composition dau tien — luc do view CHUA
    // layout nen view.height = 0 -> rect rong vinh vien (effect chi chay lai
    // khi open doi, ma open khong bao gio true neu gesture bi system cuop).
    // -> system back gesture luon thang, dai vuot cua app khong bao gio nhan
    // duoc touch. Ban moi: cap nhat rect sau moi lan layout (OnLayoutChange).
    val openState = rememberUpdatedState(open)
    DisposableEffect(view) {
        fun refreshExclusion() {
            val h = view.height
            view.systemGestureExclusionRects =
                if (!openState.value && h > 0) {
                    listOf(Rect(0, 0, (48 * density.density).toInt(), h))
                } else {
                    emptyList()
                }
        }
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            refreshExclusion()
        }
        view.addOnLayoutChangeListener(listener)
        refreshExclusion()
        onDispose {
            view.removeOnLayoutChangeListener(listener)
            view.systemGestureExclusionRects = emptyList()
        }
    }

    // Mo trang: nap lai neu du lieu cu + danh dau da xem.
    LaunchedEffect(open) {
        if (open) {
            vm.refreshIfStale()
            vm.markSeen()
        }
    }

    // Xin quyen vi tri: cho phep roi thi tao lai brief de lay thoi tiet theo vi tri.
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) vm.refresh() }
    val requestLocation: () -> Unit = {
        val has = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (has) vm.refresh() else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    // Dai vuot canh trai: mo brief. Rong 48dp = khop vung system gesture
    // exclusion, de ngon tay dat lech vao trong van trung dai vuot.
    val openThresholdPx = with(density) { 60.dp.toPx() }
    val closeThresholdPx = with(density) { 60.dp.toPx() }
    Box(modifier.fillMaxSize()) {
        if (!open) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .align(Alignment.CenterStart)
                    .pointerInput(Unit) {
                        var acc = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = { acc = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0) {
                                    acc += dragAmount
                                    if (acc > openThresholdPx) {
                                        open = true
                                        acc = 0f
                                    }
                                } else acc = 0f
                            },
                        )
                    },
            )
        }

        // Trang Brief: truot tu trai vao, vuot phai->trai de dong.
        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally(
                animationSpec = tween(400, easing = M3EMotion.emphasized),
                initialOffsetX = { -it },
            ) + fadeIn(tween(300)),
            exit = slideOutHorizontally(
                animationSpec = tween(350, easing = M3EMotion.emphasizedAcc),
                targetOffsetX = { -it },
            ) + fadeOut(tween(250)),
            modifier = Modifier.fillMaxSize(),
        ) {
            var acc = 0f
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = { acc = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount < 0) {
                                    acc -= dragAmount
                                    if (acc > closeThresholdPx) {
                                        open = false
                                        acc = 0f
                                    }
                                } else acc = 0f
                            },
                        )
                    },
            ) {
                BriefScreen(
                    cache = cache,
                    refreshing = refreshing,
                    onClose = { open = false },
                    onRequestLocation = requestLocation,
                    onRefresh = { vm.refresh() },
                )
            }
        }
    }
}

@Composable
private fun rememberBriefViewModel(): BriefViewModel {
    val app = LocalContext.current.applicationContext as Application
    val factory = remember(app) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BriefViewModel(app) as T
        }
    }
    return viewModel(factory = factory)
}
