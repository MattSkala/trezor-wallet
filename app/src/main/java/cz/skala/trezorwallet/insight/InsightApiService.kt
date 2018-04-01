package cz.skala.trezorwallet.insight

import cz.skala.trezorwallet.insight.response.AddrsTxsResponse
import cz.skala.trezorwallet.insight.response.SendTxResponse
import retrofit2.Call
import retrofit2.http.*

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

    /**
     * Estimates fee in BTC/kb for new tx to be included within the first x blocks. Can be used to
     * retrieve multiple estimates by separating blocks numbers by a comma.
     */
    @GET("utils/estimatefee")
    fun estimateFee(@Query("nbBlocks") blocks: String): Call<Map<String, Double>>

    /**
     * Broadcasts the signed transaction as hex string.
     */
    @FormUrlEncoded
    @POST("tx/send")
    fun sendTx(@Field("rawtx") rawtx: String): Call<SendTxResponse>
}