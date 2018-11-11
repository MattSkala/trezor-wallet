package cz.skala.trezorwallet.coinmarketcap

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

/**
 * A client for fetching Bitcoin exchange rate from CoinMarketCap API.
 */
class CoinMarketCapClient {
    companion object {
        private const val API_URL = "https://api.coinmarketcap.com/v1/ticker/bitcoin/"
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
        val url = "$API_URL?convert=$currency"
        val body = fetchStringBody(url)
        val jsonArray = JSONArray(body)
        val jsonItem = jsonArray.getJSONObject(0)
        val price = jsonItem.getString("price_" + currency.toLowerCase())
        return price.toDouble()
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