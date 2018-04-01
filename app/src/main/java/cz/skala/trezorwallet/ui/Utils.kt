package cz.skala.trezorwallet.ui

import java.text.NumberFormat
import java.util.*

const val BTC_TO_SATOSHI = 100000000L

fun formatBtcValue(value: Double, precision: Int = 8): String {
    val str = java.lang.String.format("%.${precision}f", value)
    var endIndex = str.length
    val dotIndex = str.indexOf(".")
    while (str[endIndex - 1] == '0' && endIndex - 1 - dotIndex > 2) {
        endIndex--
    }
    val formatted = str.substring(0, endIndex)
    return "$formatted BTC"
}

fun formatBtcValue(value: Long, precision: Int = 8): String {
    return formatBtcValue(value.toDouble() / BTC_TO_SATOSHI, precision)
}

fun formatPrice(value: Double, currencyCode: String): String {
    val format = NumberFormat.getCurrencyInstance()
    format.currency = Currency.getInstance(currencyCode)
    return format.format(value)
}

fun btcToSat(btc: Double): Long {
    return (btc * BTC_TO_SATOSHI).toLong()
}
