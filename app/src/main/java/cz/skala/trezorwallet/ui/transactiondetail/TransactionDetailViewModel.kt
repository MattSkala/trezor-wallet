package cz.skala.trezorwallet.ui.transactiondetail

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.labeling.LabelingManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A ViewModel for TransactionDetailActivity.
 */
class TransactionDetailViewModel(val database: AppDatabase, val labeling: LabelingManager) : ViewModel() {
    private lateinit var accountId: String
    private lateinit var txid: String

    val transaction = MutableLiveData<TransactionWithInOut>()

    var selectedOutput: TransactionOutput? = null

    fun start(accountId: String, txid: String) {
        if (this::txid.isInitialized) return

        this.accountId = accountId
        this.txid = txid

        launch(UI) {
            transaction.value = bg {
                database.transactionDao().getByTxid(accountId, txid)
            }.await()
        }
    }

    fun setOutputLabel(label: String) = launch(UI) {
        labeling.setOutputLabel(selectedOutput!!, label)
        transaction.value = transaction.value
        selectedOutput = null
    }

    class Factory(val database: AppDatabase, val labeling: LabelingManager) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TransactionDetailViewModel(database, labeling) as T
        }
    }
}