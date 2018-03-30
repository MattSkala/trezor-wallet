package cz.skala.trezorwallet.ui.send

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.satoshilabs.trezor.intents.ui.data.SignTxRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import cz.skala.trezorwallet.compose.TransactionComposer
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.ui.SingleLiveEvent
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A ViewModel for SendFragment.
 */
class SendViewModel(val database: AppDatabase, val prefs: PreferenceHelper) : ViewModel() {
    companion object {
        private const val TAG = "SendViewModel"
    }

    val amountBtc = MutableLiveData<Double>()
    val amountUsd = MutableLiveData<Double>()
    val trezorRequest = SingleLiveEvent<TrezorRequest>()

    private val composer = TransactionComposer(database)

    /**
     * Composes a new transaction asynchronously and returns the result in [trezorRequest].
     *
     * @param [accountId] An account to spend UTXOs from.
     * @param [address] Target Bitcoin address encoded as Base58Check.
     * @param [fee] Mining fee in satoshis per byte.
     */
    fun composeTransaction(accountId: String, address: String, amount: Double, fee: Int) {
        launch(UI) {
            val (tx, inputTransactions) = bg {
                composer.composeTransaction(accountId, address, amount, fee)
            }.await()

            val signRequest = SignTxRequest(tx, inputTransactions)
            trezorRequest.value = signRequest
        }
    }

    fun setAmountBtc(value: Double) {
        if (amountBtc.value != value) {
            amountBtc.value = value
            amountUsd.value = value * prefs.rate
        }
    }

    fun setAmountUsd(value: Double) {
        if (amountUsd.value != value) {
            amountUsd.value = value
            amountBtc.value = value / prefs.rate
        }
    }

    fun validateAddress(address: String): Boolean {
        // TODO
        return true
    }

    fun validateAmount(amount: Double): Boolean {
        // TODO
        return true
    }

    fun validateFee(fee: Int): Boolean {
        // TODO
        return true
    }

    class Factory(val database: AppDatabase, val prefs: PreferenceHelper) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SendViewModel(database, prefs) as T
        }
    }
}