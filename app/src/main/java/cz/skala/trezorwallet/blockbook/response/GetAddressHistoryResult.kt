package cz.skala.trezorwallet.blockbook.response

class GetAddressHistoryResult(
        val totalCount: Int,
        val items: List<AddressHistoryResultItem> = listOf()
)
