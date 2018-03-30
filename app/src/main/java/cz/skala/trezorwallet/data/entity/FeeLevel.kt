package cz.skala.trezorwallet.data.entity

import cz.skala.trezorwallet.R

enum class FeeLevel(val titleRes: Int, val blocks: Int, val halved: Boolean) {
    HIGH(R.string.fee_high, 2, false),
    NORMAL(R.string.fee_normal, 6, false),
    ECONOMY(R.string.fee_economy, 25, false),
    LOW(R.string.fee_low, 25, true) // 25 is max, so LOW = ECONOMY/2
}