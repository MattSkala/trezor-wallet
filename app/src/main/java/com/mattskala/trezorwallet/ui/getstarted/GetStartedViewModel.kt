package com.mattskala.trezorwallet.ui.getstarted

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.widget.Toast
import com.satoshilabs.trezor.intents.ui.data.GenericResult
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import com.mattskala.trezorwallet.crypto.ExtendedPublicKey
import com.mattskala.trezorwallet.data.AppDatabase
import com.mattskala.trezorwallet.data.PreferenceHelper
import com.mattskala.trezorwallet.data.entity.Account
import com.mattskala.trezorwallet.data.repository.TransactionRepository
import com.mattskala.trezorwallet.discovery.AccountDiscoveryManager
import com.mattskala.trezorwallet.ui.BaseViewModel
import com.mattskala.trezorwallet.ui.SingleLiveEvent
import io.socket.engineio.client.EngineIOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.generic.instance

class GetStartedViewModel(val app: Application) : BaseViewModel(app) {
    private val database: AppDatabase by instance()
    private val accountDiscovery: AccountDiscoveryManager by instance()
    private val transactionRepository: TransactionRepository by instance()
    private val prefs: PreferenceHelper by instance()

    val loading = MutableLiveData<Boolean>()
    val onPublicKeyRequest = SingleLiveEvent<PublicKeyRequest>()
    val onAccountDiscoveryFinish = SingleLiveEvent<Nothing>()

    private var legacy = false

    fun startAccountDiscovery() {
        legacy = false
        onPublicKeyRequest.value = PublicKeyRequest(0, false)
        loading.value = true
    }

    fun onPublicKeyResult(result: GenericResult) {
        if (result.state != null) {
            prefs.deviceState = result.state
        }
        val message = result.message as TrezorMessage.PublicKey
        scanAccount(message.node, message.xpub)
    }

    fun cancel() {
        loading.value = false
    }

    private fun scanAccount(node: TrezorType.HDNodeType, xpub: String) {
        viewModelScope.launch {
            val index = node.childNum - ExtendedPublicKey.HARDENED_IDX.toInt()

            try {
                val hasTransactions = withContext(Dispatchers.Default) {
                    val hasTransactions = accountDiscovery.scanAccount(node, legacy)
                    if (hasTransactions || (!legacy && index == 0)) {
                        val account = saveAccount(node, xpub, legacy)
                        transactionRepository.refresh(account.id)
                    }
                    hasTransactions
                }

                if (hasTransactions) {
                    onPublicKeyRequest.value = PublicKeyRequest(index + 1, legacy)
                } else if (!legacy) {
                    legacy = true
                    onPublicKeyRequest.value = PublicKeyRequest(0, true)
                } else {
                    prefs.initialized = true
                    onAccountDiscoveryFinish.call()
                }
            } catch (e: EngineIOException) {
                e.printStackTrace()
                Toast.makeText(app, e.message, Toast.LENGTH_SHORT).show()
                loading.value = false
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