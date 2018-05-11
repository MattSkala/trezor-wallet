package cz.skala.trezorwallet.ui.send

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.satoshilabs.trezor.intents.ui.data.SignTxRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import cz.skala.trezorwallet.compose.CoinSelector
import cz.skala.trezorwallet.compose.FeeEstimator
import cz.skala.trezorwallet.compose.TransactionComposer
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.FeeLevel
import cz.skala.trezorwallet.exception.InsufficientFundsException
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.ui.BaseViewModel
import cz.skala.trezorwallet.ui.SingleLiveEvent
import cz.skala.trezorwallet.ui.btcToSat
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.kodein.di.generic.instance
import java.io.IOException

/**
 * A ViewModel for SendFragment.
 */
class SendViewModel(app: Application) : BaseViewModel(app) {
    companion object {
        private const val TAG = "SendViewModel"
    }

    val database: AppDatabase by instance()
    val prefs: PreferenceHelper by instance()
    val feeEstimator: FeeEstimator by instance()
    val insightApi: InsightApiService by instance()
    val composer: TransactionComposer by instance()

    private var initialized = false

    val amountBtc = MutableLiveData<Double>()
    val amountUsd = MutableLiveData<Double>()
    val trezorRequest = SingleLiveEvent<TrezorRequest>()
    val recommendedFees = MutableLiveData<Map<FeeLevel, Int>>()
    val onTxSent = SingleLiveEvent<String>()
    val onInsufficientFunds = SingleLiveEvent<Nothing>()
    val sending = MutableLiveData<Boolean>()

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
     * @param [amount] Amount in satoshis to be sent to the target address.
     * @param [fee] Mining fee in satoshis per byte.
     */
    fun composeTransaction(accountId: String, address: String, amount: Long, fee: Int) {
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
        launch(UI) {
            sending.value = true
            try {
                val txid = sendTx(rawtx)

                amountBtc.value = 0.0
                amountUsd.value = 0.0

                sending.value = false
                onTxSent.value = txid
            } catch (e: Exception) {
                e.printStackTrace()
                sending.value = false
                onTxSent.value = null
            }
        }
    }

    private suspend fun sendTx(rawtx: String): String {
        return bg {
            val response = insightApi.sendTx(rawtx).execute()
            val body = response.body()

            if (!response.isSuccessful || body == null) {
                throw Exception("Sending transaction failed")
            }

            body.txid
        }.await()
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
            try {
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
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun validateAddress(address: String): Boolean {
        return validateBase58Address(address) || validateBech32Address(address);
    }

    private fun validateBase58Address(address: String): Boolean {
        return address.length in 26..35 && (address.startsWith("1") || address.startsWith("3"))
    }

    private fun validateBech32Address(address: String): Boolean {
        return address.length in 14..72 && address.startsWith("bc1")
    }

    fun validateAmount(amount: Double): Boolean {
        return btcToSat(amount) >= CoinSelector.DUST_THRESHOLD
    }

    fun validateFee(fee: Int): Boolean {
        return fee >= FeeEstimator.MINIMUM_FEE
    }
}