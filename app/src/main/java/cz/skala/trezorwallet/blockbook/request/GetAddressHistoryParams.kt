package cz.skala.trezorwallet.blockbook.request

import cz.skala.trezorwallet.blockbook.options.GetAddressHistoryOptions

class GetAddressHistoryParams(
        val addresses: List<String>,
        val options: GetAddressHistoryOptions
)