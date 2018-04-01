package cz.skala.trezorwallet.compose

import android.util.Log
import cz.skala.trezorwallet.data.entity.FeeLevel
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.ui.BTC_TO_SATOSHI
import org.jetbrains.anko.coroutines.experimental.bg

class FeeEstimator(val insightApi: InsightApiService) {
    companion object {
        private const val TAG = "FeeEstimator"

        /**
         * The minimum miner fee per byte for block inclusion.
         */
        const val MINIMUM_FEE: Int = 1

        /**
         * Estimates a total fee based on the number of inputs, outputs and desired fee per byte.
         */
        fun estimateFee(inputs: Int, outputs: Int, feeRate: Int): Int {
            return estimateTransactionSize(inputs, outputs) * feeRate
        }

        /**
         * Estimates a transaction size in bytes.
         */
        private fun estimateTransactionSize(inputs: Int, outputs: Int): Int {
            return inputs * 180 + outputs * 34 + 10
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
