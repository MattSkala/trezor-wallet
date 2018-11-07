package cz.skala.trezorwallet.blockbook

import cz.skala.trezorwallet.blockbook.response.SendTxResponse
import retrofit2.Call
import retrofit2.http.*

/**
 * An interface for communication with BitcoinBook API.
 */
interface BlockbookApiService {
    /**
     * Broadcasts the signed transaction as hex string.
     */
    @GET("sendtx/{rawtx}")
    fun sendTx(@Path("rawtx") rawtx: String): Call<SendTxResponse>
}