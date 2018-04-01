package cz.skala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.compose.FeeEstimator.Companion.estimateFee
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.exception.InsufficientFundsException
import cz.skala.trezorwallet.ui.BTC_TO_SATOSHI

class FifoCoinSelector : CoinSelector {
    /**
     * Accumulates inputs until the target value is reached.
     */
    override fun select(utxoSet: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>, feeRate: Int):
            Pair<List<TransactionOutput>, Int> {

        var outputsValue = 0L
        outputs.forEach { outputsValue += it.amount }

        val selectedUtxo = mutableListOf<TransactionOutput>()
        var inputsValue = 0L

        var fee = estimateFee(selectedUtxo.size, outputs.size, feeRate)

        for (utxo in utxoSet) {
            // We have enough funds already
            if (inputsValue >= outputsValue + fee) {
                break
            }

            selectedUtxo += utxo
            inputsValue += (utxo.value * BTC_TO_SATOSHI).toLong()
            val outputsCount = if (needsChangeOutput(selectedUtxo, outputs, feeRate))
                outputs.size + 1 else outputs.size
            fee = estimateFee(selectedUtxo.size, outputsCount, feeRate)
        }

        // Not enough funds selected
        if (inputsValue < outputsValue + fee) {
            throw InsufficientFundsException()
        }

        return Pair(selectedUtxo, fee)
    }
}