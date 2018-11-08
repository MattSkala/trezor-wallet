package cz.skala.trezorwallet.insight.response

/**
 * A transaction output.
 */
class TxOut(
        val n: Int,
        val value: Long,
        val scriptPubKey: ScriptPubKey,
        val spentHeight: Int?,
        val spentTxId: String?)