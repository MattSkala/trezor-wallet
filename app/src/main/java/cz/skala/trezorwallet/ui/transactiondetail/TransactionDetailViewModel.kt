package cz.skala.trezorwallet.ui.transactiondetail

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.data.repository.TransactionRepository
import cz.skala.trezorwallet.labeling.LabelingManager
import cz.skala.trezorwallet.ui.BaseViewModel
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

/**
 * A ViewModel for TransactionDetailActivity.
 */
class TransactionDetailViewModel(app: Application) : BaseViewModel(app) {
    private val labeling: LabelingManager by instance()
    private val transactionRepository: TransactionRepository by instance()

    private lateinit var accountId: String
    private lateinit var txid: String

    val transaction = MutableLiveData<TransactionWithInOut>()

    var selectedOutput: TransactionOutput? = null

    fun start(accountId: String, txid: String) {
        if (this::txid.isInitialized) return

        this.accountId = accountId
        this.txid = txid

        viewModelScope.launch {
            transaction.value = transactionRepository.getByTxid(accountId, txid)
        }
    }

    fun setOutputLabel(label: String) = viewModelScope.launch {
        labeling.setOutputLabel(selectedOutput!!, label)
        transaction.value = transaction.value
        selectedOutput = null
    }
}