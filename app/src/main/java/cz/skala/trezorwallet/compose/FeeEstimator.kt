package cz.skala.trezorwallet.compose

import android.util.Log
import cz.skala.trezorwallet.data.entity.FeeLevel
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.ui.BTC_TO_SATOSHI
import org.jetbrains.anko.coroutines.experimental.bg
import java.io.IOException

/**
 * A helper class for fetching recommended fee rates.
 */
class FeeEstimator(
        private val insightApi: InsightApiService
) {
    companion object {
        private const val TAG = "FeeEstimator"

        /**
         * The minimum miner fee per byte for block inclusion.
         */
        const val MINIMUM_FEE: Int = 1
    }

    /**
     * Fetches recommended fee rates for the predefined fee levels from Insight API.
     *
     * @return A map of [FeeLevel] to the recommended fee rate in satoshis per byte.
     */
    @Throws(IOException::class)
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
