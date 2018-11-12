package cz.skala.trezorwallet.ui.send

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.satoshilabs.trezor.intents.ui.data.SignTxRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import cz.skala.trezorwallet.BuildConfig
import cz.skala.trezorwallet.TrezorApplication
import cz.skala.trezorwallet.blockbook.BlockbookSocketService
import cz.skala.trezorwallet.compose.CoinSelector
import cz.skala.trezorwallet.compose.FeeEstimator
import cz.skala.trezorwallet.compose.TransactionComposer
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.FeeLevel
import cz.skala.trezorwallet.data.repository.TransactionRepository
import cz.skala.trezorwallet.exception.InsufficientFundsException
import cz.skala.trezorwallet.ui.BaseViewModel
import cz.skala.trezorwallet.ui.SingleLiveEvent
import cz.skala.trezorwallet.ui.btcToSat
import io.socket.engineio.client.EngineIOException
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

/**
 * A ViewModel for SendFragment.
 */
class SendViewModel(app: Application) : BaseViewModel(app) {
    companion object {
        private const val TAG = "SendViewModel"
    }

    private val prefs: PreferenceHelper by instance()
    private val feeEstimator: FeeEstimator by instance()
    private val blockbookSocketService: BlockbookSocketService by instance()
    private val composer: TransactionComposer by instance()
    private val transactionRepository: TransactionRepository by instance()

    private var initialized = false

    lateinit var accountId: String

    val amountBtc = MutableLiveData<Double>()
    val amountUsd = MutableLiveData<Double>()
    val trezorRequest = SingleLiveEvent<TrezorRequest>()
    val recommendedFees = MutableLiveData<Map<FeeLevel, Int>>()
    val onTxSent = SingleLiveEvent<String>()
    val onInsufficientFunds = SingleLiveEvent<Nothing>()
    val onError = SingleLiveEvent<String>()
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
        viewModelScope.launch {
            try {
                val (tx, inputTransactions) =
                        composer.composeTransaction(accountId, address, amount, fee)
                val signRequest = SignTxRequest(tx, inputTransactions, BuildConfig.COIN_NAME,
                        prefs.deviceState)

                trezorRequest.value = signRequest
            } catch (e: InsufficientFundsException) {
                onInsufficientFunds.call()
            }
        }
    }

    fun sendTransaction(rawtx: String) {
        viewModelScope.launch {
            sending.value = true
            try {
                val txid = sendTx(rawtx)

                amountBtc.value = 0.0
                amountUsd.value = 0.0

                onTxSent.value = txid
            } catch (e: Exception) {
                e.printStackTrace()
                onError.value = e.message
            } finally {
                sending.value = false
            }
        }
    }

    private suspend fun sendTx(rawtx: String): String {
        val txid = blockbookSocketService.sendTransaction(rawtx)

        // fetch new tx
        val tx = blockbookSocketService.getDetailedTransaction(txid)
        transactionRepository.saveTx(tx, accountId)

        return txid
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
        viewModelScope.launch {
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
            } catch (e: EngineIOException) {
                e.printStackTrace()
            }
        }
    }

    fun validateAddress(address: String): Boolean {
        return validateBase58Address(address) || validateBech32Address(address);
    }

    private fun validateBase58Address(address: String): Boolean {
        val validPrefix = if (TrezorApplication.isTestnet())
            address.startsWith("m") || address.startsWith("n") ||
                    address.startsWith("2") else
            address.startsWith("1") || address.startsWith("3")
        return address.length in 26..35 && validPrefix
    }

    private fun validateBech32Address(address: String): Boolean {
        val validPrefix = if (TrezorApplication.isTestnet())
            address.startsWith("tb1") else address.startsWith("bc1")
        return address.length in 14..72 && validPrefix
    }

    fun validateAmount(amount: Double): Boolean {
        return btcToSat(amount) >= CoinSelector.DUST_THRESHOLD
    }

    fun validateFee(fee: Int): Boolean {
        return fee >= FeeEstimator.MINIMUM_FEE
    }
}