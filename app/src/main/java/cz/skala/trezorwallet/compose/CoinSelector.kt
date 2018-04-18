package cz.skala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.exception.InsufficientFundsException

/**
 * An interface for coin selection algorithms.
 */
interface CoinSelector {
    companion object {
        /**
         * The minimum output value allowed when relaying transactions.
         */
        const val DUST_THRESHOLD: Long = 546
    }

    /**
     * Selects UTXOs that should be used as inputs in a new transaction.
     *
     * @param utxoSet A list of all available UTXOs sorted from the oldest.
     * @param outputs A list of the transaction outputs.
     * @param feeRate The selected fee rate in satoshis per byte.
     * @param segwit True for segwit, false for legacy accounts.
     * @return A pair of selected UTXOs and the calculated transaction fee in satoshis.
     */
    @Throws(InsufficientFundsException::class)
    fun select(utxoSet: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>,
               feeRate: Int, segwit: Boolean): Pair<List<TransactionOutput>, Int>
}