package cz.skala.trezorwallet.insight.response

/**
 * A transaction item.
 */
class Tx(
        val txid: String,
        val version: Int,
        val locktime: Int,
        val size: Int,
        val blockheight: Int,
        val blocktime: Long?,
        val valueIn: Long,
        val valueOut: Long,
        val fees: Long,
        val vin: List<TxIn>,
        val vout: List<TxOut>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tx

        if (txid != other.txid) return false

        return true
    }

    override fun hashCode(): Int {
        return txid.hashCode()
    }
}