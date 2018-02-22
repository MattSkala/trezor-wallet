package cz.skala.trezorwallet.insight.response

/**
 * Transactions for multiple addresses response.
 */
class AddrsTxsResponse(
        val totalItems: Int,
        val from: Int,
        val to: Int,
        val items: List<Tx>
)