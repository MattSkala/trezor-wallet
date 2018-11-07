package cz.skala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.compose.CoinSelector.Companion.DUST_THRESHOLD
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.exception.InsufficientFundsException

/**
 * FIFO (First In, First Out) algorithm accumulates UTXOs from the oldest, until the target is
 * reached or exceeded.
 */
class FifoCoinSelector : CoinSelector {
    override fun select(utxoSet: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>,
                        feeRate: Int, segwit: Boolean): Pair<List<TransactionOutput>, Int> {
        var target = 0L
        outputs.forEach { target += it.amount }

        val inputs = mutableListOf<TransactionOutput>()
        var inputsValue = 0L

        var fee = 0

        for (utxo in utxoSet) {
            // We have enough funds already
            if (inputsValue >= target + fee) {
                break
            }

            // Add UTXO to inputs
            inputs += utxo
            inputsValue += utxo.value

            // Update the transaction fee
            fee = calculateFee(inputs.size, outputs, feeRate, segwit)

            // If a change output is needed, increase the fee
            if (inputsValue - target - fee > DUST_THRESHOLD) {
                fee += changeOutputBytes(segwit) * feeRate
            }
        }

        // Not enough funds selected
        if (inputsValue < target + fee) {
            throw InsufficientFundsException()
        }

        return Pair(inputs, fee)
    }
}