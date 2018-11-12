package com.mattskala.trezorwallet.blockbook.request

import com.mattskala.trezorwallet.blockbook.options.GetAddressHistoryOptions

class GetAddressHistoryParams(
        val addresses: List<String>,
        val options: GetAddressHistoryOptions
)