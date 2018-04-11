package cz.skala.trezorwallet.compose

import android.util.Log
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.entity.FeeLevel
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.ui.BTC_TO_SATOSHI
import org.jetbrains.anko.coroutines.experimental.bg

class FeeEstimator(val insightApi: InsightApiService) {
    companion object {
        private const val TAG = "FeeEstimator"

        // 4 bytes version, 1 byte in count, 1 byte out count, 4 bytes locktime, 1 byte for segwit start
        private const val TX_EMPTY_SIZE = 4 + 1 + 1 + 4 + 1

        // 32 bytes tx hash, 4 bytes output index, 1 byte script size, 4 bytes sequence
        private const val TX_INPUT_BASE = 32 + 4 + 1 + 4

        // 8 bytes amount, 1 byte script size
        private const val TX_OUTPUT_BASE = 8 + 1

        private const val SEGWIT_INPUT_SCRIPT_LENGTH = 51

        private const val INPUT_SCRIPT_LENGTH = 109

        // 4 bytes ops, 1 byte hash length, 20 bytes public key hash
        private const val P2PKH_OUTPUT_SCRIPT_LENGTH = 25

        // 2 bytes ops, 1 byte hash length, 20 bytes script hash
        private const val P2SH_OUTPUT_SCRIPT_LENGTH = 23

        // 1 byte version, 1 byte hash length, 20 bytes public key hash
        private const val P2WPKH_OUTPUT_SCRIPT_LENGTH = 22

        // 1 byte version, 1 byte hash length, 32 bytes script hash
        private const val P2WSH_OUTPUT_SCRIPT_LENGTH = 34

        /**
         * The minimum miner fee per byte for block inclusion.
         */
        const val MINIMUM_FEE: Int = 1

        /**
         * Estimates the total fee based on the number of inputs, outputs and desired fee per byte.
         */
        fun estimateFee(inputs: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>, feeRate: Int, segwit: Boolean): Int {
            return estimateTransactionSize(inputs, outputs, segwit) * feeRate
        }

        /**
         * Estimates the transaction size in bytes.
         */
        fun estimateTransactionSize(inputs: List<TransactionOutput>, outputs: List<TrezorType.TxOutputType>, segwit: Boolean): Int {
            val inputsBytes = inputs.size * inputBytes(segwit)
            val outputsBytes: Int = outputs.map { outputBytes(it) }.sum()
            return TX_EMPTY_SIZE + inputsBytes + outputsBytes
        }

        fun inputBytes(segwit: Boolean): Int {
            return TX_INPUT_BASE + if (segwit) SEGWIT_INPUT_SCRIPT_LENGTH else INPUT_SCRIPT_LENGTH
        }

        fun outputBytes(segwit: Boolean): Int {
            return TX_OUTPUT_BASE + if (segwit) P2SH_OUTPUT_SCRIPT_LENGTH else P2PKH_OUTPUT_SCRIPT_LENGTH
        }

        fun outputBytes(output: TrezorType.TxOutputType): Int {
            return outputBytes(output.scriptType == TrezorType.OutputScriptType.PAYTOP2SHWITNESS)
        }
    }

    /**
     * Fetches recommended fees for the predefined fee levels from Insight API.
     */
    suspend fun fetchRecommendedFees(): Map<FeeLevel, Int>? {
        return bg {
            // Block numbers separated by a comma
            val feeLevelsQuery = FeeLevel.values()
                    .filter { !it.halved }
                    .joinToString(",") { it.blocks.toString() }

            // Send a request to Insight API
            val response = insightApi.estimateFee(feeLevelsQuery).execute()
            val body = response.body()

            if (response.isSuccessful && body != null) {
                val recommendedFees = mutableMapOf<FeeLevel, Int>()

                FeeLevel.values().forEach {
                    var btcPerKb = body.getOrDefault(it.blocks.toString(), 0.0)

                    // Calculate the low fee as the half of economy
                    if (it.halved) {
                        btcPerKb /= 2
                    }

                    // Convert BTC/kB to sat/B
                    var satPerB = (btcPerKb * BTC_TO_SATOSHI / 1000).toInt()

                    // Set minimal fee as 1 sat/B
                    satPerB = Math.max(satPerB, MINIMUM_FEE)

                    recommendedFees[it] = satPerB
                }

                recommendedFees
            } else {
                Log.e(TAG, "Fetching recommended fees failed")
                null
            }
        }.await()
    }
}
