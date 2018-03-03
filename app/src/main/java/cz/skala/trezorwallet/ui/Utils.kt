package cz.skala.trezorwallet.ui

fun formatBtcValue(value: Double): String {
    var str = java.lang.String.format("%.8f", value)
    var endIndex = str.length
    val dotIndex = str.indexOf(".")
    while (str[endIndex - 1] == '0' && endIndex - 1 - dotIndex > 2) {
        endIndex--
    }
    val formatted = str.substring(0, endIndex)
    return formatted + " BTC"
}