package cz.skala.trezorwallet.ui

import android.app.Application
import android.arch.lifecycle.*
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import cz.skala.trezorwallet.labeling.LabelingManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A ViewModel for MainActivity.
 */
class MainViewModel(app: Application, val database: AppDatabase, val labeling: LabelingManager, val prefs: PreferenceHelper) : AndroidViewModel(app) {
    val accounts: LiveData<List<Account>> by lazy {
        database.accountDao().getAllLiveData()
    }

    val selectedAccount = MutableLiveData<Account>()
    val labelingEnabled = MutableLiveData<Boolean>()

    val onTrezorRequest = SingleLiveEvent<TrezorRequest>()
    val onLastAccountEmpty = SingleLiveEvent<Nothing>()

    private var isAccountRequestLegacy = false

    init {
        labelingEnabled.value = labeling.isEnabled()
    }

    fun setSelectedAccount(account: Account) {
        if (selectedAccount.value != account) {
            selectedAccount.value = account
        }
    }

    fun addAccount(legacy: Boolean) {
        launch(UI) {
            val lastAccount = bg {
                 database.accountDao().getAll().lastOrNull { it.legacy == legacy }
            }.await()

            val lastAccountTransactions = if (lastAccount != null) {
                bg {
                    database.transactionDao().getByAccount(lastAccount.id).size
                }.await()
            } else 0

            if (lastAccountTransactions > 0) {
                val newIndex = if (lastAccount != null) lastAccount.index + 1 else 0
                isAccountRequestLegacy = legacy
                onTrezorRequest.value =
                        AccountDiscoveryManager.createGetPublicKeyRequest(newIndex, legacy)
            } else {
                onLastAccountEmpty.call()
            }
        }
    }

    fun saveAccount(node: TrezorType.HDNodeType, xpub: String) {
        launch(UI) {
            bg {
                val account = Account.fromNode(node, xpub, isAccountRequestLegacy)
                database.accountDao().insert(account)
            }.await()
        }
    }

    fun enableLabeling(masterKey: ByteArray) = launch(UI) {
        labeling.setMasterKey(masterKey)
        bg {
            labeling.fetchAccountsMetadata()
        }.await()
        labelingEnabled.value = true
    }

    fun disableLabeling() = launch(UI) {
        labeling.disableLabeling()
        labelingEnabled.value = false
    }

    /**
     * Updates currently selected account label.
     */
    fun setAccountLabel(label: String) = launch(UI) {
        val account = selectedAccount.value!!
        labeling.setAccountLabel(account, label)
    }

    fun forgetDevice() = launch(UI) {
        labeling.disableLabeling()
        bg {
            database.accountDao().deleteAll()
            database.transactionDao().deleteAll()
            database.addressDao().deleteAll()
            prefs.clear()
        }.await()
    }

    class Factory(val app: Application, val database: AppDatabase, val labeling: LabelingManager,
                  val prefs: PreferenceHelper) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(app, database, labeling, prefs) as T
        }
    }
}