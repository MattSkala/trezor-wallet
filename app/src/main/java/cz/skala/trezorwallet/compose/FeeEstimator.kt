package cz.skala.trezorwallet.compose

import android.util.Log
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.entity.FeeLevel
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.ui.BTC_TO_SATOSHI
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A helper class for fetching recommended fee rates and estimating the fee based on
 * the transaction structure.
 */
class FeeEstimator(
        private val insightApi: InsightApiService
) {
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
        internal const val P2PKH_OUTPUT_SCRIPT_LENGTH = 25

        // 2 bytes ops, 1 byte hash length, 20 bytes script hash
        internal const val P2SH_OUTPUT_SCRIPT_LENGTH = 23

        // 1 byte version, 1 byte hash length, 20 bytes public key hash
        internal const val P2WPKH_OUTPUT_SCRIPT_LENGTH = 22

        // 1 byte version, 1 byte hash length, 32 bytes script hash
        internal const val P2WSH_OUTPUT_SCRIPT_LENGTH = 34

        /**
         * The length of the Bech32 P2WPKH address.
         */
        private const val P2WPKH_ADDRESS_LENGTH = 42

        /**
         * The length of the Bech32 P2SH address.
         */
        private const val P2WSH_ADDRESS_LENGTH = 62

        /**
         * The minimum miner fee per byte for block inclusion.
         */
        const val MINIMUM_FEE: Int = 1

        /**
         * Estimates the fee of a composed transaction, based on the number and type of inputs,
         * outputs and a desired fee per byte.
         *
         * @param inputsCount Number of transaction inputs.
         * @param outputs Transaction outputs.
         * @param feeRate Fee rate in satoshis per byte.
         * @param segwit True for segwit, false for legacy accounts.
         */
        fun estimateFee(inputsCount: Int, outputs: List<TrezorType.TxOutputType>,
                        feeRate: Int, segwit: Boolean): Int {
            return estimateTransactionSize(inputsCount, outputs, segwit) * feeRate
        }

        /**
         * Estimates the transaction size in bytes.
         */
        private fun estimateTransactionSize(inputsCount: Int,
                                            outputs: List<TrezorType.TxOutputType>,
                                            segwit: Boolean): Int {
            val inputsBytes = inputsCount * inputBytes(segwit)
            val outputsBytes: Int = outputs.map { outputBytes(it) }.sum()
            return TX_EMPTY_SIZE + inputsBytes + outputsBytes
        }

        /**
         * Returns an input size in bytes.
         *
         * @param segwit True for segwit, false for legacy accounts.
         */
        private fun inputBytes(segwit: Boolean): Int {
            val scriptLength = if (segwit) SEGWIT_INPUT_SCRIPT_LENGTH else INPUT_SCRIPT_LENGTH
            return TX_INPUT_BASE + scriptLength
        }

        /**
         * Returns a change output size in bytes.
         *
         * @param segwit True for segwit, false for legacy accounts.
         */
        fun changeOutputBytes(segwit: Boolean): Int {
            val scriptLength = if (segwit) P2SH_OUTPUT_SCRIPT_LENGTH else P2PKH_OUTPUT_SCRIPT_LENGTH
            return TX_OUTPUT_BASE + scriptLength
        }

        /**
         * Returns and output size in bytes.
         */
        private fun outputBytes(output: TrezorType.TxOutputType): Int {
            return TX_OUTPUT_BASE + scriptLength(output)
        }

        /**
         * Returns the unlocking script length in bytes according to the output type.
         */
        fun scriptLength(output: TrezorType.TxOutputType): Int {
            return when {
                output.scriptType == TrezorType.OutputScriptType.PAYTOP2SHWITNESS ->
                    P2SH_OUTPUT_SCRIPT_LENGTH
                output.scriptType == TrezorType.OutputScriptType.PAYTOWITNESS ->
                    P2WPKH_OUTPUT_SCRIPT_LENGTH
                output.scriptType == TrezorType.OutputScriptType.PAYTOADDRESS -> {
                    when {
                        output.address.startsWith("1") -> P2PKH_OUTPUT_SCRIPT_LENGTH
                        output.address.startsWith("3") -> P2SH_OUTPUT_SCRIPT_LENGTH
                        output.address.startsWith("bc1") -> {
                            when (output.address.length) {
                                P2WPKH_ADDRESS_LENGTH -> P2WPKH_OUTPUT_SCRIPT_LENGTH
                                P2WSH_ADDRESS_LENGTH -> P2WSH_OUTPUT_SCRIPT_LENGTH
                                else -> 0
                            }
                        }
                        else -> 0
                    }
                }
                else -> 0
            }
        }
    }

    /**
     * Fetches recommended fee rates for the predefined fee levels from Insight API.
     *
     * @return A map of [FeeLevel] to the recommended fee rate in satoshis per byte.
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
