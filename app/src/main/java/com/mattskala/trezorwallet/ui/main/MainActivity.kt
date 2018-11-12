package com.mattskala.trezorwallet.ui.main

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.dropbox.core.android.Auth
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.mattskala.trezorwallet.R
import com.mattskala.trezorwallet.data.PreferenceHelper
import com.mattskala.trezorwallet.data.entity.Account
import com.mattskala.trezorwallet.data.item.AccountItem
import com.mattskala.trezorwallet.data.item.AccountSectionItem
import com.mattskala.trezorwallet.data.item.AddAccountItem
import com.mattskala.trezorwallet.data.item.Item
import com.mattskala.trezorwallet.labeling.LabelingManager
import com.mattskala.trezorwallet.ui.BaseActivity
import com.mattskala.trezorwallet.ui.LabelDialogFragment
import com.mattskala.trezorwallet.ui.addresses.AddressesFragment
import com.mattskala.trezorwallet.ui.getstarted.GetStartedActivity
import com.mattskala.trezorwallet.ui.send.SendFragment
import com.mattskala.trezorwallet.ui.transactions.TransactionsFragment
import kotlinx.android.synthetic.main.activity_main.*
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import kotlin.reflect.KClass


class MainActivity : BaseActivity(), LabelDialogFragment.EditTextDialogListener {
    companion object {
        private const val TAG = "MainActivity"

        private const val ITEM_ACCOUNT_LABEL = 5
        private const val ITEM_FORGET = 10
        private const val REQUEST_GET_PUBLIC_KEY = 2
        private const val REQUEST_ENABLE_LABELING = 3
    }

    private val viewModel: MainViewModel by instance()
    private val prefs: PreferenceHelper by instance()

    private val accountsAdapter = AccountsAdapter()

    private var dropboxAuthRequested = false

    override fun provideOverridingModule() = Kodein.Module("Main") {
        bind<MainViewModel>() with provider {
            ViewModelProviders.of(this@MainActivity)[MainViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!prefs.initialized) {
            startGetStartedActivity()
            return
        }

        setContentView(R.layout.activity_main)

        initToolbar()

        accountsAdapter.onItemClickListener = {
            viewModel.setSelectedAccount(it)
            drawerLayout.closeDrawers()
        }

        accountsAdapter.onAddAccountListener = {
            viewModel.addAccount(it)
        }

        accountsList.adapter = accountsAdapter
        accountsList.layoutManager = LinearLayoutManager(this)

        btnLabeling.setOnClickListener {
            when (viewModel.labelingState.value) {
                MainViewModel.LabelingState.ENABLED -> viewModel.disableLabeling()
                MainViewModel.LabelingState.DISABLED -> enableLabeling()
                else -> {}
            }
        }

        viewModel.accounts.observe(this, Observer {
            if (it != null) {
                if (it.isNotEmpty()) {
                    showAccounts(it)
                    val selectedAccount = viewModel.selectedAccount.value
                    if (selectedAccount == null || !it.contains(selectedAccount)) {
                        val newSelectedAccount = it.first()
                        viewModel.setSelectedAccount(newSelectedAccount)
                    }
                } else {
                    forgetDevice()
                }
            }
        })

        viewModel.selectedAccount.observe(this, Observer {
            if (it != null) {
                accountsAdapter.selectedAccount = it
                accountsAdapter.notifyDataSetChanged()
                navigation.selectedItemId = viewModel.selectedTab
                supportActionBar?.title = it.getDisplayLabel(resources)
            }
        })

        viewModel.labelingState.observe(this, Observer {
            if (it != null) {
                invalidateOptionsMenu()
                btnLabeling.setText(when (it) {
                    MainViewModel.LabelingState.ENABLED -> R.string.disable_labeling
                    MainViewModel.LabelingState.DISABLED -> R.string.enable_labeling
                    MainViewModel.LabelingState.SYNCING -> R.string.syncing
                })
            }
        })

        viewModel.onTrezorRequest.observe(this, Observer {
            if (it != null) {
                val intent = TrezorActivity.createIntent(this, it)
                startActivityForResult(intent, REQUEST_GET_PUBLIC_KEY)
            }
        })

        viewModel.onLastAccountEmpty.observe(this, Observer {
            Toast.makeText(applicationContext, R.string.last_account_empty,
                    Toast.LENGTH_LONG).show()
        })

        navigation.setOnNavigationItemSelectedListener {
            val account = viewModel.selectedAccount.value
            if (account != null) {
                handleNavigationItemSelected(it.itemId, account)
            }
            viewModel.selectedTab = it.itemId
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (viewModel.labelingState.value == MainViewModel.LabelingState.ENABLED) {
            val accountLabel = menu.add(0, ITEM_ACCOUNT_LABEL, 0, R.string.account_label)
            accountLabel.setIcon(R.drawable.ic_tag_white_24dp)
            accountLabel.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menu.add(0, ITEM_FORGET, 1, R.string.forget_device)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            ITEM_ACCOUNT_LABEL -> {
                showAccountLabelDialog()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_GET_PUBLIC_KEY -> if (resultCode == Activity.RESULT_OK) {
                val message = TrezorActivity.getMessage(data) as TrezorMessage.PublicKey
                val node = message.node
                val xpub = message.xpub
                viewModel.saveAccount(node, xpub)
            }
            REQUEST_ENABLE_LABELING -> if (resultCode == Activity.RESULT_OK) {
                val message = TrezorActivity.getMessage(data) as TrezorMessage.CipheredKeyValue
                val masterKey = message.value.toByteArray()
                viewModel.enableLabeling(masterKey)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onResume() {
        super.onResume()

        navigation.selectedItemId = viewModel.selectedTab

        if (dropboxAuthRequested) {
            val dropboxToken = Auth.getOAuth2Token()
            if (dropboxToken != null) {
                viewModel.setDropboxToken(dropboxToken)
                sendLabelingMasterKeyRequest()
            }
            dropboxAuthRequested = false
        }
    }

    override fun onTextChanged(text: String) {
        viewModel.setAccountLabel(text)
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
    }

    private fun handleNavigationItemSelected(itemId: Int, account: Account) {
        val accountId = account.id
        when (itemId) {
            R.id.item_transactions -> showTransactions(accountId)
            R.id.item_receive -> showAddresses(accountId)
            R.id.item_send -> showSend(accountId)
        }
    }

    private fun forgetDevice() {
        viewModel.forgetDevice()
        startGetStartedActivity()
    }

    private fun startGetStartedActivity() {
        val intent = Intent(this, GetStartedActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showAccounts(accounts: List<Account>) {
        accountsAdapter.items = createAccountItems(accounts)
        accountsAdapter.notifyDataSetChanged()
    }

    private fun createAccountItems(accounts: List<Account>): List<Item> {
        val segwitAccounts = accounts.filter { !it.legacy }
        val legacyAccounts = accounts.filter { it.legacy }

        val items = mutableListOf<Item>()

        segwitAccounts.forEach {
            items.add(AccountItem(it))
        }
        items.add(AddAccountItem(false))

        items.add(AccountSectionItem(R.string.legacy_accounts))
        legacyAccounts.forEach {
            items.add(AccountItem(it))
        }
        items.add(AddAccountItem(true))

        return items
    }

    fun showTransactions() {
        navigation.selectedItemId = R.id.item_transactions
    }

    private fun showTransactions(accountId: String) {
        showFragment(TransactionsFragment::class, accountId)
    }

    private fun showAddresses(accountId: String) {
        showFragment(AddressesFragment::class, accountId)
    }

    private fun showSend(accountId: String) {
        showFragment(SendFragment::class, accountId)
    }

    private fun createFragment(klass: KClass<out Fragment>, accountId: String): Fragment {
        val f = klass.java.newInstance()
        val args = Bundle()
        args.putString("account_id", accountId)
        f.arguments = args
        return f
    }

    private fun showFragment(klass: KClass<out Fragment>, accountId: String) {
        val tag = klass.java.name + "_" + accountId
        val f = supportFragmentManager.findFragmentByTag(tag)
                ?: createFragment(klass, accountId)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.content, f, tag)
        ft.commit()
    }

    private fun enableLabeling() {
        dropboxAuthRequested = true
        Auth.startOAuth2Authentication(this, getString(R.string.dropbox_app_key))
    }

    private fun sendLabelingMasterKeyRequest() {
        val intent = TrezorActivity.createIntent(this,
                LabelingManager.createCipherKeyValueRequest())
        startActivityForResult(intent, REQUEST_ENABLE_LABELING)
    }

    private fun showAccountLabelDialog() {
        val fragment = LabelDialogFragment()
        val title = resources.getString(R.string.account_label)
        val label = viewModel.selectedAccount.value!!.getDisplayLabel(resources)
        val args = Bundle()
        args.putString(LabelDialogFragment.ARG_TITLE, title)
        args.putString(LabelDialogFragment.ARG_TEXT, label)
        fragment.arguments = args
        fragment.show(supportFragmentManager, "dialog")
    }
}