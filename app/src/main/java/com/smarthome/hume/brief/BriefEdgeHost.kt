package com.smarthome.hume.brief

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.ui.components.M3EMotion

/**
 * Host cua trang Brief o tang root (2026-09-30: doi sang MO bang vuot ngang
 * sang phai TREN THANH NAVBAR — thay cho vuot canh trai, de tranh nham voi
 * system back gesture cua thiet bi; user chon):
 * - M3ERootScreen giu state open, truyen vao day; M3ENavBar goi mo qua callback.
 * - Tren trang Brief: vuot phai->trai de DONG; nut back / system back khi mo
 *   -> dong trang (khong thoat app); nut dong tren trang.
 * - Phu ca navbar khi mo (lop tren cung).
 */
@Composable
fun BriefEdgeHost(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val vm: BriefViewModel = rememberBriefViewModel()
    val density = LocalDensity.current

    val cache by vm.cache.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()

    // Back (nut + gesture) khi trang mo -> dong trang.
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(open, dispatcher) {
        val cb = object : androidx.activity.OnBackPressedCallback(open) {
            override fun handleOnBackPressed() { onOpenChange(false) }
        }
        dispatcher?.addCallback(cb)
        onDispose { cb.remove() }
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

    // Nguong vuot de DONG trang (vuot phai->trai tren trang Brief).
    val closeThresholdPx = with(density) { 60.dp.toPx() }
    Box(modifier.fillMaxSize()) {
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
                                        onOpenChange(false)
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
                    onClose = { onOpenChange(false) },
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
