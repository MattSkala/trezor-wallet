package com.mattskala.trezorwallet.coingecko

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * A client for fetching Bitcoin exchange rate from CoinGecko API.
 */
class CoinGeckoClient {
    companion object {
        private const val API_URL = "https://api.coingecko.com/api/v3/coins/bitcoin"
    }

    private val client = OkHttpClient()

    /**
     * Fetches a BTC/fiat exchange rate.
     *
     * @param currency The currency code.
     * @return The BTC/fiat exchange rate.
     */
    @Throws(IOException::class)
    suspend fun fetchRate(currency: String): Double {
        val url = "$API_URL?tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false"
        val body = fetchStringBody(url)
        val json = JSONObject(body)
        return json.getJSONObject("market_data")
                .getJSONObject("current_price")
                .getDouble(currency.toLowerCase())
    }

    @Throws(IOException::class)
    private suspend fun fetchStringBody(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code: $response")
        val body = response.body() ?: throw IOException("Response body is null")
        body.string()
    }
}