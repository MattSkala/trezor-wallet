package cz.skala.trezorwallet.insight.response

/**
 * A transaction input.
 */
class TxIn(
        val addr: String,
        val txid: String,
        val value: Double,
        val valueSat: Long)