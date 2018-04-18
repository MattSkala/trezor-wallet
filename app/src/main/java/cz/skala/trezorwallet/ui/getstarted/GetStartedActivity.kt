package cz.skala.trezorwallet.ui.getstarted

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyResult
import com.satoshilabs.trezor.intents.ui.data.InitializeRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.TrezorApplication
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import cz.skala.trezorwallet.ui.MainActivity
import kotlinx.android.synthetic.main.activity_get_started.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.defaultSharedPreferences


/**
 * An introductory activity where account discovery is performed after TREZOR device is connected.
 */
class GetStartedActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "GetStartedActivity"

        private const val REQUEST_INITIALIZE = 1
        private const val REQUEST_GET_PUBLIC_KEY = 2
    }

    private val injector = KodeinInjector()
    private val accountDiscovery: AccountDiscoveryManager by injector.instance()
    private val database: AppDatabase by injector.instance()

    private var legacy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(appKodein())

        setContentView(R.layout.activity_get_started)

        btnContinue.setOnClickListener {
            initializeConnection()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_INITIALIZE -> if (resultCode == Activity.RESULT_OK) {
                val result = TrezorActivity.getResult(data)
                val message = result!!.message as TrezorMessage.Features
                getPublicKeyForAccount(0, false)
            }
            REQUEST_GET_PUBLIC_KEY -> if (resultCode == Activity.RESULT_OK) {
                val result = TrezorActivity.getResult(data) as GetPublicKeyResult
                val node = result.message.node
                val xpub = result.message.xpub
                scanAccount(node, xpub)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun initializeConnection() {
        val intent = TrezorActivity.createIntent(this,
                InitializeRequest(TrezorMessage.Initialize.getDefaultInstance()))
        TrezorActivity.createIntent(this,
                TrezorRequest(TrezorMessage.Initialize.getDefaultInstance()))
        startActivityForResult(intent, REQUEST_INITIALIZE)
    }

    private fun getPublicKeyForAccount(i: Int, legacy: Boolean) {
        this.legacy = legacy
        val request = AccountDiscoveryManager.createGetPublicKeyRequest(i, legacy)
        val intent = TrezorActivity.createIntent(this, request)
        startActivityForResult(intent, REQUEST_GET_PUBLIC_KEY)
    }

    private fun scanAccount(node: TrezorType.HDNodeType, xpub: String) {
        launch(UI) {
            val index = node.childNum - ExtendedPublicKey.HARDENED_IDX.toInt()

            val hasTransactions = bg {
                val hasTransactions = accountDiscovery.scanAccount(node, legacy)
                if (hasTransactions || (!legacy && index == 0)) {
                    saveAccount(node, xpub, legacy)
                }
                hasTransactions
            }.await()

            if (hasTransactions) {
                getPublicKeyForAccount(index + 1, legacy)
            } else if (!legacy) {
                getPublicKeyForAccount(0, true)
            } else {
                finishAccountDiscovery()
            }
        }
    }

    private fun saveAccount(node: TrezorType.HDNodeType, xpub: String, legacy: Boolean) {
        val account = Account.fromNode(node, xpub, legacy)
        database.accountDao().insert(account)
    }

    private fun finishAccountDiscovery() {
        defaultSharedPreferences.edit()
                .putBoolean(TrezorApplication.PREF_INITIALIZED, true).apply()

        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}