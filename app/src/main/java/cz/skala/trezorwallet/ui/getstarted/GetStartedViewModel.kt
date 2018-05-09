package cz.skala.trezorwallet.ui.getstarted

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.satoshilabs.trezor.intents.ui.data.GenericResult
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.data.repository.TransactionRepository
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import cz.skala.trezorwallet.ui.BaseViewModel
import cz.skala.trezorwallet.ui.SingleLiveEvent
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.kodein.di.generic.instance

class GetStartedViewModel(app: Application) : BaseViewModel(app) {
    val database: AppDatabase by instance()
    val accountDiscovery: AccountDiscoveryManager by instance()
    val transactionRepository: TransactionRepository by instance()
    val prefs: PreferenceHelper by instance()

    val onPublicKeyRequest = SingleLiveEvent<PublicKeyRequest>()
    val onAccountDiscoveryFinish = SingleLiveEvent<Nothing>()

    private var legacy = false

    fun startAccountDiscovery() {
        legacy = false
        onPublicKeyRequest.value = PublicKeyRequest(0, false)
    }

    fun onPublicKeyResult(result: GenericResult) {
        if (result.state != null) {
            prefs.deviceState = result.state
        }
        val message = result.message as TrezorMessage.PublicKey
        scanAccount(message.node, message.xpub)
    }

    private fun scanAccount(node: TrezorType.HDNodeType, xpub: String) {
        launch(UI) {
            val index = node.childNum - ExtendedPublicKey.HARDENED_IDX.toInt()

            val hasTransactions = bg {
                val hasTransactions = accountDiscovery.scanAccount(node, legacy)
                if (hasTransactions || (!legacy && index == 0)) {
                    val account = saveAccount(node, xpub, legacy)
                    transactionRepository.refresh(account.id)
                }
                hasTransactions
            }.await()

            if (hasTransactions) {
                onPublicKeyRequest.value = PublicKeyRequest(index + 1, legacy)
            } else if (!legacy) {
                legacy = true
                onPublicKeyRequest.value = PublicKeyRequest(0, true)
            } else {
                prefs.initialized = true
                onAccountDiscoveryFinish.call()
            }
        }
    }

    private fun saveAccount(node: TrezorType.HDNodeType, xpub: String, legacy: Boolean): Account {
        val account = Account.fromNode(node, xpub, legacy)
        database.accountDao().insert(account)
        return account
    }

    class PublicKeyRequest(val i: Int, val legacy: Boolean)
}