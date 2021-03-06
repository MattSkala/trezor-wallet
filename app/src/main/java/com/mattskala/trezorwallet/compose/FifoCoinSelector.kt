package com.mattskala.trezorwallet.compose

import com.satoshilabs.trezor.lib.protobuf.TrezorType
import com.mattskala.trezorwallet.compose.CoinSelector.Companion.DUST_THRESHOLD
import com.mattskala.trezorwallet.data.entity.TransactionOutput
import com.mattskala.trezorwallet.exception.InsufficientFundsException
import com.mattskala.trezorwallet.sumByLong

/**
 * FIFO (First In, First Out) algorithm accumulates UTXOs from the oldest, until the target is
 * reached or exceeded.
 */
class FifoCoinSelector : CoinSelector {
    override fun select(utxoSet: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>,
                        feeRate: Int, segwit: Boolean): Pair<List<TransactionOutput>, Int> {
        var target = outputs.sumByLong { it.amount }

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