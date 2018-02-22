package cz.skala.trezorwallet.insight.response

/**
 * A transaction item.
 */
class Tx(
        val txid: String,
        val version: Int,
        val locktime: Int,
        val blockhash: String,
        val confirmations: Int,
        val time: Long,
        val blocktime: Long,
        val size: Int,
        val valueIn: Double,
        val valueOut: Double,
        val fees: Double,
        val vin: List<TxIn>,
        val vout: List<TxOut>
)