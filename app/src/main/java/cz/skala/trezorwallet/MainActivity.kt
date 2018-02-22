package cz.skala.trezorwallet

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyRequest
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyResult
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.insight.response.AddrsTxsResponse
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.security.InvalidKeyException





class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        private const val REQUEST_GET_PUBLIC_KEY = 1
    }

    private var accountIndex = 0

    private lateinit var insightApi: InsightApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: move to application
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        insightApi = createInsightApiService()

        Log.d(TAG, "onCreate")

        setContentView(R.layout.activity_main)

        btnAccountDiscovery.setOnClickListener {
            discoverAccount(0)
        }

        btnFetchTransactions.setOnClickListener {
            val addresses = mutableListOf<String>()
            addresses.add("13Sed6Hr1Gfoz6gNPQ8CpF8hMcL92G7knv")
            addresses.add("1L6kamXM4GfiU5FEnFnqLef5zPK4vzvSgd")
            addresses.add("128sCPddjEdzddDbsmQMi143tdm8Fbdcrz")
            addresses.add("1J7t4Y4pXXhv2eqRzJQhRaeuaxxozTEDiX")
            addresses.add("1AAgdW9ieFb8EAzd75L7VaaEDoUZCCARwa")
            fetchTransactions(addresses)
        }
    }

    private fun createInsightApiService(): InsightApiService {
        val httpClient = OkHttpClient.Builder()

        // logging interceptor
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        httpClient.addInterceptor(logging)

        val retrofit = Retrofit.Builder()
                .baseUrl("https://btc-bitcore1.trezor.io/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()

        return retrofit.create(InsightApiService::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_GET_PUBLIC_KEY -> if (resultCode == RESULT_OK) {
                val result = TrezorActivity.getResult(data) as GetPublicKeyResult

                AlertDialog.Builder(this)
                        .setMessage(result.publicKey.xpub)
                        .show()

                discoverAddressesForAccount(result.publicKey.node)

                /*
                if (accountIndex < 2) {
                    discoverAccount(++accountIndex)
                }
                */
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun discoverAccount(i: Int) {
        val purpose = ExtendedPublicKey.HARDENED_IDX + 44 // BIP44
        val coinType = ExtendedPublicKey.HARDENED_IDX + 0 // Bitcoin

        Log.d(TAG, "Account #" + i)
        val account = ExtendedPublicKey.HARDENED_IDX + i
        val path = intArrayOf(purpose.toInt(), coinType.toInt(), account.toInt())
        val intent = TrezorActivity.createIntent(this@MainActivity,
                GetPublicKeyRequest(path, i == 0))
        startActivityForResult(intent, REQUEST_GET_PUBLIC_KEY)
    }

    private fun discoverAddressesForAccount(node: TrezorType.HDNodeType) {
        try {
            val accountNode = ExtendedPublicKey(
                    ExtendedPublicKey.decodePublicKey(node.publicKey.toByteArray()),
                    node.chainCode.toByteArray())
            val externalChainNode = accountNode.deriveChildKey(0)

            val addresses = mutableListOf<String>()
            for (addressIndex in 0..20) {
                val addressNode = externalChainNode.deriveChildKey(addressIndex)
                val address = addressNode.getAddress()
                addresses.add(address)
                Log.d(TAG, "/$addressIndex $address")
            }

            fetchTransactions(addresses)
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
    }

    private fun fetchTransactions(addresses: List<String>) {
        insightApi.getAddrsTxs(addresses.joinToString(","))
                .enqueue(object : Callback<AddrsTxsResponse> {
                    override fun onResponse(call: Call<AddrsTxsResponse>, response: Response<AddrsTxsResponse>) {
                        val body = response.body()
                        if (response.isSuccessful && body != null) {
                            var received = 0.0
                            var sent = 0.0
                            var unspent = 0.0

                            body.items.forEach { tx ->
                                tx.vout.forEach { txOut ->
                                    txOut.scriptPubKey.addresses?.forEach { addr ->
                                        if (addresses.contains(addr)) {
                                            received += txOut.value.toDouble()

                                            if (txOut.spentHeight != null) {
                                                sent += txOut.value.toDouble()
                                            } else {
                                                unspent += txOut.value.toDouble()
                                            }
                                        }
                                    }
                                }
                            }

                            Log.d("MainActivity", "totalItems:" + body.totalItems)
                            Log.d("MainActivity", "received:" + received)
                            Log.d("MainActivity", "sent:" + sent)
                            Log.d("MainActivity", "unspent:" + unspent)
                        }
                    }

                    override fun onFailure(call: Call<AddrsTxsResponse>, t: Throwable) {
                        t.printStackTrace()
                    }
                })
    }
}