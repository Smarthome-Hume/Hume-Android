package com.smarthome.hume.feature.energy.powerwall

/**
 * Route cho trang Powerwall 3D (N-02).
 *
 * DIEM NOI DIEU HUONG (cho worker N-05): trang nay KHONG tu gan vao
 * NavHost — N-05 chi can them 1 destination dung route nay, vi du:
 *
 *     composable(PowerwallRoute) { PowerwallScreen(onBack = { navController.popBackStack() }) }
 *
 * Contract mo trang (port iOS M3EEnergyView.swift:227-229):
 * iOS mo bang `.navigationDestination(isPresented: showPowerwall)` tu the
 * pin / node pin tren flow card; trang Powerwall tu dong ve Home khi an
 * pill title "Powerwall" (onBack o day) hoac vuot back he thong.
 */
const val PowerwallRoute = "powerwall"
