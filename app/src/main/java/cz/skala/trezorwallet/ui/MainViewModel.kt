package cz.skala.trezorwallet.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A ViewModel for MainActivity.
 */
class MainViewModel(val database: AppDatabase) : ViewModel() {
    val accounts: LiveData<List<Account>> by lazy {
        database.accountDao().getAllLiveData()
    }

    val selectedAccount = MutableLiveData<Account>()

    val onTrezorRequest = SingleLiveEvent<TrezorRequest>()
    val onLastAccountEmpty = SingleLiveEvent<Nothing>()

    private var isAccountRequestLegacy = false

    class Factory(val database: AppDatabase) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(database) as T
        }
    }

    fun setSelectedAccount(account: Account) {
        if (selectedAccount.value != account) {
            selectedAccount.value = account
        }
    }

    fun addAccount(legacy: Boolean) {
        launch(UI) {
            val lastAccount = bg {
                 database.accountDao().getAll().last { it.legacy == legacy }
            }.await()

            val lastAccountTransactions = bg {
                database.transactionDao().getByAccount(lastAccount.id).size
            }.await()

            if (lastAccountTransactions > 0) {
                val newIndex = lastAccount.index + 1
                isAccountRequestLegacy = legacy
                onTrezorRequest.value =
                        AccountDiscoveryManager.createGetPublicKeyRequest(newIndex, legacy)
            } else {
                onLastAccountEmpty.call()
            }
        }
    }

    fun saveAccount(node: TrezorType.HDNodeType) {
        launch(UI) {
            bg {
                val account = Account.fromNode(node, isAccountRequestLegacy)
                database.accountDao().insert(account)
            }.await()
        }
    }
}