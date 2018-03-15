package cz.skala.trezorwallet.ui.transactiondetail

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A ViewModel for TransactionDetailActivity.
 */
class TransactionDetailViewModel(val database: AppDatabase) : ViewModel() {
    private lateinit var txid: String

    val transaction = MutableLiveData<TransactionWithInOut>()

    fun start(txid: String) {
        if (this::txid.isInitialized) return

        this.txid = txid

        launch(UI) {
            transaction.value = bg {
                database.transactionDao().getByTxid(txid)
            }.await()
        }
    }

    class Factory(val database: AppDatabase) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TransactionDetailViewModel(database) as T
        }
    }
}