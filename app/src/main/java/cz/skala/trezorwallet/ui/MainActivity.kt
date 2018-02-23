package cz.skala.trezorwallet.ui

import android.arch.persistence.room.Room
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyRequest
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyResult
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.BuildConfig
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.discovery.TransactionFetcher
import cz.skala.trezorwallet.insight.InsightApiService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.anko.coroutines.experimental.bg
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

    private lateinit var transactionFetcher: TransactionFetcher

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: move to application
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        insightApi = createInsightApiService()

        transactionFetcher = TransactionFetcher()
        transactionFetcher.insightApi = insightApi

        database = Room.databaseBuilder(applicationContext,
                AppDatabase::class.java, "trezor-wallet").build()

        Log.d(TAG, "onCreate")

        setContentView(R.layout.activity_main)

        btnAccountDiscovery.setOnClickListener {
            discoverAccount(0)
        }

        btnFetchTransactions.setOnClickListener {
            async(UI) {
                val balance = bg {
                    val accounts = database.accountDao().getAll()
                    Log.d(TAG, "accounts count: " + accounts.size)
                    var balance = -1.0
                    if (accounts.isNotEmpty()) {
                        val account = accounts[0]
                        val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(account.publicKey), account.chainCode)
                        val (txs, addresses) = transactionFetcher.fetchTransactionsForAccount(accountNode)
                        balance = transactionFetcher.calculateBalance(txs, addresses)
                    }
                    balance
                }.await()
                Toast.makeText(this@MainActivity, "Balance: $balance BTC", Toast.LENGTH_SHORT).show()
            }
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
            val publicKey = node.publicKey.toByteArray()
            val chainCode = node.chainCode.toByteArray()

            val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(publicKey), chainCode)

            async (UI) {
                val balance = bg {
                    val account = Account(accountNode.getAddress(), publicKey, chainCode, 0, true, null)
                    database.accountDao().insert(account)

                    val (txs, addresses) = transactionFetcher.fetchTransactionsForAccount(accountNode)
                    transactionFetcher.calculateBalance(txs, addresses)
                }.await()
                Toast.makeText(this@MainActivity, "Balance: $balance BTC", Toast.LENGTH_SHORT).show()
            }
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
    }
}