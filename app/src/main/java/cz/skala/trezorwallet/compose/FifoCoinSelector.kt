package cz.skala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.compose.FeeEstimator.Companion.estimateFee
import cz.skala.trezorwallet.compose.FeeEstimator.Companion.outputBytes
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.exception.InsufficientFundsException

class FifoCoinSelector : CoinSelector {
    /**
     * Accumulates inputs until the target value is reached.
     */
    override fun select(utxoSet: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>, feeRate: Int, segwit: Boolean):
            Pair<List<TransactionOutput>, Int> {

        var outputsValue = 0L
        outputs.forEach { outputsValue += it.amount }

        val selectedUtxo = mutableListOf<TransactionOutput>()
        var inputsValue = 0L

        var fee = 0

        for (utxo in utxoSet) {
            // We have enough funds already
            if (inputsValue >= outputsValue + fee) {
                break
            }

            selectedUtxo += utxo
            inputsValue += utxo.value

            fee = estimateFee(selectedUtxo, outputs, feeRate, segwit)
            if (needsChangeOutput(selectedUtxo, outputs, feeRate, segwit)) {
                val changeOutputSize = outputBytes(segwit)
                fee += changeOutputSize * feeRate
            }
        }

        // Not enough funds selected
        if (inputsValue < outputsValue + fee) {
            throw InsufficientFundsException()
        }

        return Pair(selectedUtxo, fee)
    }
}