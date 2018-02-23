package cz.skala.trezorwallet.insight

import cz.skala.trezorwallet.insight.response.AddrsTxsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * An interface for communication with Bitcore Insight API.
 */
interface InsightApiService {
    /**
     * Get transactions for multiple addresses.
     */
    @GET("addrs/{addrs}/txs")
    fun getAddrsTxs(@Path("addrs") addrs: String, @Query("from") from: Int,
                    @Query("to") to: Int): Call<AddrsTxsResponse>
}