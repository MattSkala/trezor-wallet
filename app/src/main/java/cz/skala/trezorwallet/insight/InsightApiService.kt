package cz.skala.trezorwallet.insight

import cz.skala.trezorwallet.insight.response.AddrResponse
import cz.skala.trezorwallet.insight.response.AddrsTxsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * An interface for communication with Bitcore Insight API.
 */
interface InsightApiService {
    /**
     * Get details for a single address.
     */
    @GET("addr/{addr}")
    fun getAddr(@Path("addr") addr: String): Call<AddrResponse>

    /**
     * Get transactions for multiple addresses.
     */
    @GET("addrs/{addrs}/txs")
    fun getAddrsTxs(@Path("addrs") addrs: String): Call<AddrsTxsResponse>
}