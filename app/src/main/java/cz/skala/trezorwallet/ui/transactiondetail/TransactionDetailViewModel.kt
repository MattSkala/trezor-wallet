package cz.skala.trezorwallet.ui.transactiondetail

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.labeling.LabelingManager
import cz.skala.trezorwallet.ui.BaseViewModel
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.kodein.di.generic.instance

/**
 * A ViewModel for TransactionDetailActivity.
 */
class TransactionDetailViewModel(app: Application) : BaseViewModel(app) {
    val database: AppDatabase by instance()
    val labeling: LabelingManager by instance()

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
}