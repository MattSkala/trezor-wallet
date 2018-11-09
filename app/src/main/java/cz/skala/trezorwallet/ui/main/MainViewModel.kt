package cz.skala.trezorwallet.ui.main

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.blockbook.BlockbookSocketService
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import cz.skala.trezorwallet.labeling.LabelingManager
import cz.skala.trezorwallet.ui.BaseViewModel
import cz.skala.trezorwallet.ui.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

/**
 * A ViewModel for MainActivity.
 */
class MainViewModel(app: Application) : BaseViewModel(app) {
    val database: AppDatabase by instance()
    val labeling: LabelingManager by instance()
    val prefs: PreferenceHelper by instance()
    val blockbookSocketService: BlockbookSocketService by instance()

    val accounts: LiveData<List<Account>> by lazy {
        database.accountDao().getAllLiveData()
    }

    val selectedAccount = MutableLiveData<Account>()
    var selectedTab = R.id.item_transactions
    val labelingState = MutableLiveData<LabelingState>()

    val onTrezorRequest = SingleLiveEvent<TrezorRequest>()
    val onLastAccountEmpty = SingleLiveEvent<Nothing>()

    private var isAccountRequestLegacy = false

    init {
        labelingState.value = if (labeling.isEnabled())
            LabelingState.ENABLED else LabelingState.DISABLED

        if (labeling.isEnabled()) {
            GlobalScope.launch(Dispatchers.Default) {
                labeling.downloadAccountsMetadata()
            }
        }

        blockbookSocketService.subscribe("bitcoind/hashblock")
    }

    enum class LabelingState {
        DISABLED, SYNCING, ENABLED
    }

    override fun onCleared() {
        blockbookSocketService.disconnect()
        super.onCleared()
    }

    fun setSelectedAccount(account: Account) {
        if (selectedAccount.value != account) {
            selectedTab = R.id.item_transactions
            selectedAccount.value = account
        }
    }

    fun addAccount(legacy: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            val lastAccount = async(Dispatchers.Default) {
                 database.accountDao().getAll().lastOrNull { it.legacy == legacy }
            }.await()

            val lastAccountTransactions = if (lastAccount != null) {
                async(Dispatchers.Default) {
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
        GlobalScope.launch(Dispatchers.Default) {
            val account = Account.fromNode(node, xpub, isAccountRequestLegacy)
            database.accountDao().insert(account)
        }
    }

    fun enableLabeling(masterKey: ByteArray) = GlobalScope.launch(Dispatchers.Main) {
        labelingState.value = LabelingState.SYNCING
        labeling.enableLabeling(masterKey)
        labelingState.value = LabelingState.ENABLED
    }

    fun disableLabeling() = GlobalScope.launch(Dispatchers.Main) {
        labeling.disableLabeling()
        labelingState.value = LabelingState.DISABLED
    }

    fun setDropboxToken(token: String) {
        labeling.setDropboxToken(token)
    }

    /**
     * Updates currently selected account label.
     */
    fun setAccountLabel(label: String) = GlobalScope.launch(Dispatchers.Main) {
        val account = selectedAccount.value!!
        labeling.setAccountLabel(account, label)
    }

    fun forgetDevice() = GlobalScope.launch(Dispatchers.Main) {
        if (labeling.isEnabled()) {
            labeling.disableLabeling()
        }
        async(Dispatchers.Default) {
            database.accountDao().deleteAll()
            database.transactionDao().deleteAll()
            database.addressDao().deleteAll()
            prefs.clear()
        }.await()
    }
}