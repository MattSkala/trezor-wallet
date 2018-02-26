package cz.skala.trezorwallet.discovery

import android.arch.persistence.room.Room
import android.content.Context
import android.util.Log
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.insight.InsightApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.InvalidKeyException


/**
 * Account discovery algorithm as defined in BIP 44.
 */
class AccountDiscoveryManager(context: Context) {
    companion object {
        const val GAP_SIZE = 20
        const val TAG = "AccountDiscoveryManager"
    }

    private val insightApi: InsightApiService
    private val transactionFetcher: TransactionFetcher
    private val database: AppDatabase

    init {
        insightApi = createInsightApiService()

        transactionFetcher = TransactionFetcher()
        transactionFetcher.insightApi = insightApi

        database = Room.databaseBuilder(context,
                AppDatabase::class.java, "trezor-wallet").build()
    }

    fun startAccountDiscovery() {
    }

    /**
     * Scans account external chain addresses for transactions.
     * @return True if any transactions are found, false otherwise.
     */
    fun scanAccount(node: TrezorType.HDNodeType): Boolean {
        try {
            val publicKey = node.publicKey.toByteArray()
            val chainCode = node.chainCode.toByteArray()

            val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(publicKey), chainCode)
            val externalChainNode = accountNode.deriveChildKey(0)

            return scanTransactionsForChainNode(externalChainNode)
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
        return false
    }

    private fun scanTransactionsForChainNode(externalChainNode: ExtendedPublicKey): Boolean {
        val addrs = mutableListOf<String>()
        for (addressIndex in 0 until GAP_SIZE) {
            val addressNode = externalChainNode.deriveChildKey(addressIndex)
            val address = addressNode.getAddress()
            addrs.add(address)
            Log.d(TAG, "/$addressIndex $address")
        }
        val page = transactionFetcher.fetchTransactionsPage(addrs, 0, 50)
        return page.isNotEmpty()
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
}