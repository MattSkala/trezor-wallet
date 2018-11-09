package cz.skala.trezorwallet.data.entity

import cz.skala.trezorwallet.R

enum class FeeLevel(val titleRes: Int, val blocks: Int) {
    HIGH(R.string.fee_high, 2),
    NORMAL(R.string.fee_normal, 6),
    ECONOMY(R.string.fee_economy, 25),
    LOW(R.string.fee_low, 50) // 25 is max, so LOW = ECONOMY/2
}