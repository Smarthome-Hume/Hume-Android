package com.smarthome.hume.core.ui.components

import androidx.compose.ui.text.TextStyle

/**
 * Tabular numbers: cac chu so co do rong bang nhau, giup so lieu
 * khong nhay khi cap nhat (dong ho, bieu do, stat).
 * Thay the 8 cho dung fontFeatureSettings = "tnum" truc tiep
 * va ban private trong EnergyConsTab.kt.
 */
fun TextStyle.tnum(): TextStyle = copy(fontFeatureSettings = "tnum")
