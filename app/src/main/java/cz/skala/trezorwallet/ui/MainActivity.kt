package cz.skala.trezorwallet.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.TrezorApplication
import cz.skala.trezorwallet.data.AppDatabase
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.defaultSharedPreferences


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        private const val ITEM_FORGET = 10
    }

    private val injector = KodeinInjector()
    private val database: AppDatabase by injector.instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(appKodein())

        if (!defaultSharedPreferences.getBoolean(TrezorApplication.PREF_INITIALIZED, false)) {
            startGetStartedActivity()
            return
        }

        setContentView(R.layout.activity_main)

        /*
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
        */
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, ITEM_FORGET, 0, "Forget device")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ITEM_FORGET -> {
                forgetDevice()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun forgetDevice() {
        async(UI) {
            bg {
                database.accountDao().deleteAll()
                defaultSharedPreferences.edit()
                        .putBoolean(TrezorApplication.PREF_INITIALIZED, false).apply()
            }.await()
            startGetStartedActivity()
        }
    }

    private fun startGetStartedActivity() {
        val intent = Intent(this, GetStartedActivity::class.java)
        startActivity(intent)
        finish()
    }

    /*
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
    */
}