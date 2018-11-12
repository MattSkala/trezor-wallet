package com.mattskala.trezorwallet.blockbook.options

class GetAddressHistoryOptions(
        val start: Int,
        val end: Int = 0,
        val from: Int = 0,
        val to: Int = Int.MAX_VALUE,
        val queryMempoolOnly: Boolean = false
)