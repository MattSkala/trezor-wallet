package cz.skala.trezorwallet.data.entity

import cz.skala.trezorwallet.R

enum class FeeLevel(val titleRes: Int, val blocks: Int) {
    HIGH(R.string.fee_high, 2), // asap
    NORMAL(R.string.fee_normal, 6), // 1 hour
    ECONOMY(R.string.fee_economy, 36), // 6 hours
    LOW(R.string.fee_low, 432) // 3 days
}