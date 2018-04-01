package cz.skala.trezorwallet.ui.send

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.util.Log
import com.satoshilabs.trezor.intents.ui.data.SignTxRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import cz.skala.trezorwallet.compose.FeeEstimator
import cz.skala.trezorwallet.compose.TransactionComposer
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.FeeLevel
import cz.skala.trezorwallet.exception.InsufficientFundsException
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.insight.response.SendTxResponse
import cz.skala.trezorwallet.ui.SingleLiveEvent
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * A ViewModel for SendFragment.
 */
class SendViewModel(
        val database: AppDatabase,
        val prefs: PreferenceHelper,
        val feeEstimator: FeeEstimator,
        val insightApi: InsightApiService,
        val composer: TransactionComposer
) : ViewModel() {
    companion object {
        private const val TAG = "SendViewModel"
    }

    private var initialized = false

    val amountBtc = MutableLiveData<Double>()
    val amountUsd = MutableLiveData<Double>()
    val trezorRequest = SingleLiveEvent<TrezorRequest>()
    val recommendedFees = MutableLiveData<Map<FeeLevel, Int>>()
    val onTxSent = SingleLiveEvent<Boolean>()
    val onInsufficientFunds = SingleLiveEvent<Nothing>()

    fun start() {
        if (!initialized) {
            initRecommendedFees()
            fetchRecommendedFees()
            initialized = true
        }
    }

    /**
     * Composes a new transaction asynchronously and returns the result in [trezorRequest].
     *
     * @param [accountId] An account to spend UTXOs from.
     * @param [address] Target Bitcoin address encoded as Base58Check.
     * @param [fee] Mining fee in satoshis per byte.
     */
    fun composeTransaction(accountId: String, address: String, amount: Double, fee: Int) {
        launch(UI) {
            try {
                val (tx, inputTransactions) = bg {
                    composer.composeTransaction(accountId, address, amount, fee)
                }.await()
                val signRequest = SignTxRequest(tx, inputTransactions)
                trezorRequest.value = signRequest
            } catch (e: InsufficientFundsException) {
                onInsufficientFunds.call()
            }
        }
    }

    fun sendTransaction(rawtx: String) {
        insightApi.sendTx(rawtx).enqueue(object : Callback<SendTxResponse> {
            override fun onResponse(call: Call<SendTxResponse>?, response: Response<SendTxResponse>?) {
                if (response?.isSuccessful == true) {
                    val res = response.body()
                    val txid = res?.txid
                    Log.d(TAG, "sendTx: $txid")

                    amountBtc.value = 0.0
                    amountUsd.value = 0.0

                    // TODO: clear form
                    // TODO: save transaction

                    onTxSent.value = true
                } else {
                    onTxSent.value = false
                }
            }

            override fun onFailure(call: Call<SendTxResponse>?, t: Throwable?) {
                t?.printStackTrace()
                onTxSent.value = false
            }
        })
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

    private fun initRecommendedFees() {
        recommendedFees.value = mapOf(
                FeeLevel.HIGH to prefs.feeHigh,
                FeeLevel.NORMAL to prefs.feeNormal,
                FeeLevel.ECONOMY to prefs.feeEconomy,
                FeeLevel.LOW to prefs.feeLow
        )
    }

    private fun fetchRecommendedFees() {
        launch(UI) {
            val fees = feeEstimator.fetchRecommendedFees()
            if (fees != null) {
                fees[FeeLevel.HIGH]?.let {
                    prefs.feeHigh = it
                }
                fees[FeeLevel.NORMAL]?.let {
                    prefs.feeNormal = it
                }
                fees[FeeLevel.ECONOMY]?.let {
                    prefs.feeEconomy = it
                }
                fees[FeeLevel.LOW]?.let {
                    prefs.feeLow = it
                }
                recommendedFees.value = fees
            }
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

    class Factory(val database: AppDatabase, val prefs: PreferenceHelper,
                  val feeEstimator: FeeEstimator, val insightApi: InsightApiService,
                  val composer: TransactionComposer) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SendViewModel(database, prefs, feeEstimator, insightApi, composer) as T
        }
    }
}