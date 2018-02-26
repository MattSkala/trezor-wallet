package cz.skala.trezorwallet.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.ActivityInjector
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.TrezorApplication
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.defaultSharedPreferences


class MainActivity : AppCompatActivity(), ActivityInjector {
    companion object {
        private const val TAG = "MainActivity"

        private const val ITEM_FORGET = 10
    }

    override val injector = KodeinInjector()
    private val database: AppDatabase by injector.instance()
    private val viewModel: MainViewModel by injector.instance()

    private lateinit var accountsAdapter: AccountsAdapter

    override fun provideOverridingModule() = Kodein.Module {
        bind<MainViewModel>() with provider {
            val factory = MainViewModel.Factory(instance())
            ViewModelProviders.of(this@MainActivity, factory)[MainViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeInjector()

        if (!defaultSharedPreferences.getBoolean(TrezorApplication.PREF_INITIALIZED, false)) {
            startGetStartedActivity()
            return
        }

        setContentView(R.layout.activity_main)

        initToolbar()

        accountsAdapter = AccountsAdapter()
        accountsList.adapter = accountsAdapter
        accountsList.layoutManager = LinearLayoutManager(this)

        viewModel.accounts.observe(this, Observer {
            if (it != null) {
                showAccounts(it)
            }
        })

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

    override fun onDestroy() {
        destroyInjector()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, ITEM_FORGET, 0, "Forget device")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            ITEM_FORGET -> {
                forgetDevice()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
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

    private fun showAccounts(accounts: List<Account>) {
        accountsAdapter.accounts = accounts
        accountsAdapter.notifyDataSetChanged()
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