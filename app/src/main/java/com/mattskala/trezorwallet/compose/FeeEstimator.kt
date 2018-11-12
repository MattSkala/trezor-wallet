package com.mattskala.trezorwallet.compose

import com.mattskala.trezorwallet.blockbook.BlockbookSocketService
import com.mattskala.trezorwallet.data.entity.FeeLevel
import com.mattskala.trezorwallet.ui.BTC_TO_SATOSHI
import io.socket.engineio.client.EngineIOException

/**
 * A helper class for fetching recommended fee rates.
 */
class FeeEstimator(
        private val blockbookSocketService: BlockbookSocketService
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
    @Throws(EngineIOException::class)
    suspend fun fetchRecommendedFees(): Map<FeeLevel, Int>? {
        val recommendedFees = mutableMapOf<FeeLevel, Int>()

        FeeLevel.values().forEach {
            val btcPerKb = blockbookSocketService.estimateSmartFee(it.blocks, false)

            // Convert BTC/kB to sat/B
            var satPerB = (btcPerKb * BTC_TO_SATOSHI / 1000).toInt()

            // Set minimal fee as 1 sat/B
            satPerB = Math.max(satPerB, MINIMUM_FEE)

            recommendedFees[it] = satPerB
        }

        return recommendedFees
    }
}
