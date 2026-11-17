package com.smarthome.hume.feature.energy.powerwall

/**
 * Route cho trang Powerwall 3D (N-02).
 *
 * DIEM NOI DIEU HUONG (N-05 da land): trang nay KHONG gan vao NavHost
 * (root Android khong co NavHost) — mo bang boolean `powerwallShown` nhu
 * iOS `.navigationDestination(isPresented: showPowerwall)`, tat bang
 * onBack. Route nay chi la dinh danh, khong phai destination NavHost.
 * (Sua comment stale: ban truoc ghi "cho worker N-05" khi N-05 chua lam.)
 *
 * Contract mo trang (port iOS M3EEnergyView.swift:227-229):
 * iOS mo bang `.navigationDestination(isPresented: showPowerwall)` tu the
 * pin / node pin tren flow card; trang Powerwall tu dong ve Home khi an
 * pill title "Powerwall" (onBack o day) hoac vuot back he thong.
 */
const val PowerwallRoute = "powerwall"
