package cz.skala.trezorwallet.insight.response

/**
 * A transaction input.
 */
class TxIn(
        val n: Int,
        val addr: String?,
        val txid: String,
        val vout: Int,
        val value: Long,
        val scriptSig: ScriptSig,
        val sequence: Long)