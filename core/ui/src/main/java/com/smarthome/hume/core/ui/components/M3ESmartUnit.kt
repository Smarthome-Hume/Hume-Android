package com.smarthome.hume.core.ui.components

import java.util.Locale
import kotlin.math.abs

/**
 * Format so thong minh: tu dong doi don vi khi gia tri lon.
 *
 * VND: 200 -> "200 VND", 20000 -> "20k VND", 2000000 -> "2tr VND"
 * Cong suat: 500 -> "500 W", 1500 -> "1.5 kW", 2000000 -> "2 MW"
 */

/** Format so: nguyen thi khong thap phan, le thi toi da 2 chu so, cat so 0 thua. */
private fun formatSmartNumber(v: Double): String {
    if (v % 1.0 == 0.0) return v.toLong().toString()
    return String.format(Locale("vi"), "%.2f", v).trimEnd('0').trimEnd('.')
}

/**
 * Tien VND thong minh.
 * 200 -> "200 VND" | 20000 -> "20k VND" | 2500000 -> "2.5tr VND"
 */
fun Long.toSmartVnd(): String {
    val (v, u) = toSmartVndParts()
    return "$v $u"
}

/** Tach gia tri va don vi: 2500000 -> ("2.5tr", "VND"). */
fun Long.toSmartVndParts(): Pair<String, String> {
    val a = abs(this.toDouble())
    return when {
        a >= 1_000_000 -> formatSmartNumber(this / 1_000_000.0) + "tr" to "VND"
        a >= 1_000 -> formatSmartNumber(this / 1_000.0) + "k" to "VND"
        else -> this.toString() to "VND"
    }
}

/** Ban Double cua [toSmartVnd]. */
fun Double.toSmartVnd(): String {
    val (v, u) = toSmartVndParts()
    return "$v $u"
}

/** Tach gia tri va don vi (ban Double). */
fun Double.toSmartVndParts(): Pair<String, String> {
    val a = abs(this)
    return when {
        a >= 1_000_000 -> formatSmartNumber(this / 1_000_000.0) + "tr" to "VND"
        a >= 1_000 -> formatSmartNumber(this / 1_000.0) + "k" to "VND"
        else -> formatSmartNumber(this) to "VND"
    }
}

/**
 * Cong suat thong minh (W -> kW -> MW).
 * 500 -> "500 W" | 1500 -> "1.5 kW" | 2000000 -> "2 MW"
 */
fun Double.toSmartPower(): String {
    val (v, u) = toSmartPowerParts()
    return "$v $u"
}

/** Tach gia tri va don vi: 1500.0 -> ("1.5", "kW"). */
fun Double.toSmartPowerParts(): Pair<String, String> {
    val a = abs(this)
    return when {
        a >= 1_000_000 -> formatSmartNumber(this / 1_000_000) to "MW"
        a >= 1_000 -> formatSmartNumber(this / 1_000) to "kW"
        else -> formatSmartNumber(this) to "W"
    }
}

/** Ban Long cua [toSmartPower]. */
fun Long.toSmartPower(): String = this.toDouble().toSmartPower()

/** Ban Long cua [toSmartPowerParts]. */
fun Long.toSmartPowerParts(): Pair<String, String> = this.toDouble().toSmartPowerParts()

/**
 * Nang luong thong minh (Wh -> kWh -> MWh).
 * 500 -> "500 Wh" | 1500 -> "1.5 kWh" | 2000000 -> "2 MWh"
 */
fun Double.toSmartEnergy(): String {
    val a = abs(this)
    return when {
        a >= 1_000_000 -> "${formatSmartNumber(this / 1_000_000)} MWh"
        a >= 1_000 -> "${formatSmartNumber(this / 1_000)} kWh"
        else -> "${formatSmartNumber(this)} Wh"
    }
}

/** Ban Long cua [toSmartEnergy]. */
fun Long.toSmartEnergy(): String = this.toDouble().toSmartEnergy()
