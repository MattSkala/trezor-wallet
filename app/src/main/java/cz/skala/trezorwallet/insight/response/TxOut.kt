package cz.skala.trezorwallet.insight.response

/**
 * A transaction output.
 */
class TxOut(
        val value: String,
        val scriptPubKey: ScriptPubKey,
        val spentHeight: Int?,
        val spentTxId: String?)