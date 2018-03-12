package cz.skala.trezorwallet.ui.getstarted

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyRequest
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyResult
import com.satoshilabs.trezor.intents.ui.data.InitializeRequest
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
import kotlinx.coroutines.experimental.async
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

        private const val PURPOSE_BIP44 = 44
        private const val PURPOSE_BIP49 = 49
        private const val COIN_BITCOIN = 0
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
                getPublicKeyForAccount(0, false)
            }
            REQUEST_GET_PUBLIC_KEY -> if (resultCode == Activity.RESULT_OK) {
                val result = TrezorActivity.getResult(data) as GetPublicKeyResult
                val node = result.message.node
                scanAccount(node)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun initializeConnection() {
        val intent = TrezorActivity.createIntent(this, InitializeRequest())
        startActivityForResult(intent, REQUEST_INITIALIZE)
    }

    private fun getPublicKeyForAccount(i: Int, legacy: Boolean) {
        val purpose = if (legacy) ExtendedPublicKey.HARDENED_IDX + PURPOSE_BIP44 else
            ExtendedPublicKey.HARDENED_IDX + PURPOSE_BIP49
        val coinType = ExtendedPublicKey.HARDENED_IDX + COIN_BITCOIN
        val account = ExtendedPublicKey.HARDENED_IDX + i
        val message = TrezorMessage.GetPublicKey.newBuilder()
                .addAddressN(purpose.toInt())
                .addAddressN(coinType.toInt())
                .addAddressN(account.toInt())
                .build()
        this.legacy = legacy
        Log.d(TAG, "getPublicKeyForAccount: " + i + " " + legacy + " => " +
                (purpose - ExtendedPublicKey.HARDENED_IDX) + " " +
                (coinType - ExtendedPublicKey.HARDENED_IDX) + " " +
                (account - ExtendedPublicKey.HARDENED_IDX))
        val intent = TrezorActivity.createIntent(this, GetPublicKeyRequest(message))
        startActivityForResult(intent, REQUEST_GET_PUBLIC_KEY)
    }

    private fun scanAccount(node: TrezorType.HDNodeType) {
        async(UI) {
            val index = node.childNum - ExtendedPublicKey.HARDENED_IDX.toInt()

            val hasTransactions = bg {
                val hasTransactions = accountDiscovery.scanAccount(node, legacy)
                if (hasTransactions || (!legacy && index == 0)) {
                    saveAccount(node, legacy)
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

    private fun saveAccount(node: TrezorType.HDNodeType, legacy: Boolean) {
        val publicKey = node.publicKey.toByteArray()
        val chainCode = node.chainCode.toByteArray()
        val index = node.childNum - ExtendedPublicKey.HARDENED_IDX.toInt()
        val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(publicKey), chainCode)
        val account = Account(accountNode.getAddress(), publicKey, chainCode, index,
                legacy, null, 0)
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