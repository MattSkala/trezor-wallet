package cz.skala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.compose.FeeEstimator.Companion.estimateFee
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.exception.InsufficientFundsException

interface CoinSelector {
    companion object {
        /**
         * The minimum output value allowed when relaying transactions.
         */
        const val MINIMUM_OUTPUT_VALUE: Long = 546
    }

    /**
     * Selects unspent transactions output that should be used as inputs in the new transaction.
     */
    @Throws(InsufficientFundsException::class)
    fun select(utxoSet: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>, feeRate: Int):
            Pair<List<TransactionOutput>, Int>

    /**
     * Returns whether we should add change output to the transaction. If the change output would
     * be smaller than the network would accept, we leave it as an increased miner fee.
     */
    fun needsChangeOutput(inputs: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>,
                          feeRate: Int): Boolean {
        val fee = estimateFee(inputs.size, outputs.size, feeRate)

        var inputsValue = 0L
        inputs.forEach { inputsValue += it.value }

        var outputsValue = 0L
        outputs.forEach { outputsValue += it.amount }

        val change = inputsValue - outputsValue - fee

        return (change >= MINIMUM_OUTPUT_VALUE)
    }
}