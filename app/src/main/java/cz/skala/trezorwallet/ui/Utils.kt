package cz.skala.trezorwallet.ui

import java.text.NumberFormat
import java.util.*

fun formatBtcValue(value: Double, precision: Int = 8): String {
    var str = java.lang.String.format("%.${precision}f", value)
    var endIndex = str.length
    val dotIndex = str.indexOf(".")
    while (str[endIndex - 1] == '0' && endIndex - 1 - dotIndex > 2) {
        endIndex--
    }
    val formatted = str.substring(0, endIndex)
    return formatted + " BTC"
}

fun formatPrice(value: Double, currencyCode: String): String {
    val format = NumberFormat.getCurrencyInstance()
    format.currency = Currency.getInstance(currencyCode)
    return format.format(value)
}