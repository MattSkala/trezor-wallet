package cz.skala.trezorwallet.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.AppCompatActivityInjector
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.TrezorApplication
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.ui.addresses.AddressesFragment
import cz.skala.trezorwallet.ui.getstarted.GetStartedActivity
import cz.skala.trezorwallet.ui.transactions.TransactionsFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.defaultSharedPreferences


class MainActivity : AppCompatActivity(), AppCompatActivityInjector {
    companion object {
        private const val TAG = "MainActivity"

        private const val ITEM_FORGET = 10
    }

    override val injector = KodeinInjector()
    private val database: AppDatabase by injector.instance()
    private val viewModel: MainViewModel by injector.instance()

    private val accountsAdapter = AccountsAdapter()

    override fun provideOverridingModule() = Kodein.Module {
        bind<MainViewModel>() with provider {
            val factory = MainViewModel.Factory(instance())
            ViewModelProviders.of(this@MainActivity, factory)[MainViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initializeInjector()
        super.onCreate(savedInstanceState)

        if (!defaultSharedPreferences.getBoolean(TrezorApplication.PREF_INITIALIZED, false)) {
            startGetStartedActivity()
            return
        }

        setContentView(R.layout.activity_main)

        initToolbar()

        accountsAdapter.onItemClickListener = {
            viewModel.selectedAccountPosition.value = accountsAdapter.selectedPosition
            drawerLayout.closeDrawers()
        }

        accountsList.adapter = accountsAdapter
        accountsList.layoutManager = LinearLayoutManager(this)

        viewModel.accounts.observe(this, Observer {
            if (it != null) {
                if (it.isNotEmpty()) {
                    showAccounts(it)
                    showSelectedAccount()
                } else {
                    forgetDevice()
                }
            }
        })

        viewModel.selectedAccountPosition.observe(this, Observer {
            if (it != null) {
                showSelectedAccount()
            }
        })

        navigation.setOnNavigationItemSelectedListener {
            val accounts = viewModel.accounts.value!!
            val position = viewModel.selectedAccountPosition.value!!
            val accountId = accounts[position].id
            when (it.itemId) {
                R.id.item_transactions -> showTransactions(accountId)
                R.id.item_receive -> showAddresses(accountId)
                R.id.item_send -> replaceFragment(Fragment())
            }
            true
        }
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
                viewModel.accounts.removeObservers(this)
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
        launch(UI) {
            bg {
                database.accountDao().deleteAll()
                database.transactionDao().deleteAll()
                database.addressDao().deleteAll()
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

    private fun showTransactions(accountId: String) {
        val f = TransactionsFragment()
        val args = Bundle()
        args.putString(TransactionsFragment.ARG_ACCOUNT_ID, accountId)
        f.arguments = args
        replaceFragment(f)
    }

    private fun showAddresses(accountId: String) {
        val f = AddressesFragment()
        val args = Bundle()
        args.putString(AddressesFragment.ARG_ACCOUNT_ID, accountId)
        f.arguments = args
        replaceFragment(f)
    }

    private fun showSelectedAccount() {
        val accounts = viewModel.accounts.value
        val selectedAccountPosition = viewModel.selectedAccountPosition.value
        if (accounts != null && selectedAccountPosition != null && accounts.size > selectedAccountPosition) {
            showTransactions(accounts[selectedAccountPosition].id)
        }

        if (navigation.selectedItemId != R.id.item_transactions) {
            navigation.selectedItemId = R.id.item_transactions
        }
    }

    private fun replaceFragment(f: Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.content, f)
        ft.commit()
    }
}