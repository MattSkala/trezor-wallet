package cz.skala.trezorwallet.insight.response

/**
 * Address details response.
 */
class AddrResponse(
        val addrStr: String,
        val balance: Double,
        val balanceSat: Long,
        val totalReceived: Double,
        val totalReceivedSat: Long,
        val totalSent: Double,
        val totalSentSat: Long,
        val transactions: List<String>
)