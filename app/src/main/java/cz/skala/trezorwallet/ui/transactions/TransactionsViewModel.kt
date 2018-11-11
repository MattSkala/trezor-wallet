package cz.skala.trezorwallet.ui.transactions

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import cz.skala.trezorwallet.coinmarketcap.CoinMarketCapClient
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.data.item.AccountSummaryItem
import cz.skala.trezorwallet.data.item.DateItem
import cz.skala.trezorwallet.data.item.Item
import cz.skala.trezorwallet.data.item.TransactionItem
import cz.skala.trezorwallet.data.repository.TransactionRepository
import cz.skala.trezorwallet.discovery.BalanceCalculator
import cz.skala.trezorwallet.ui.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import java.io.IOException


/**
 * A ViewModel for TransactionsFragment.
 */
class TransactionsViewModel(app: Application) : BaseViewModel(app), KodeinAware {
    private val database: AppDatabase by instance()
    private val coinMarketCapClient: CoinMarketCapClient by instance()
    private val prefs: PreferenceHelper by instance()
    private val transactionRepository: TransactionRepository by instance()
    private val balanceCalculator: BalanceCalculator by instance()

    val items = MutableLiveData<List<Item>>()
    val refreshing = MutableLiveData<Boolean>()
    val empty = MutableLiveData<Boolean>()
    val account = MutableLiveData<Account>()

    private var initialized = false
    private lateinit var accountId: String
    private var transactions = listOf<TransactionWithInOut>()
    private var summary = AccountSummary(0L, 0L)

    private val transactionsLiveData by lazy {
        transactionRepository.getByAccountLiveDataWithInOut(accountId)
    }

    private val transactionsObserver = Observer<List<TransactionWithInOut>> { txs ->
        if (txs != null) {
            transactions = txs.sortedWith(compareBy({ it.tx.blockheight == -1 },
                    { it.tx.blockheight })).reversed()
            summary = balanceCalculator.createAccountSummary(txs)
            updateItems()
        }
    }

    fun start(accountId: String) {
        if (!initialized) {
            this.accountId = accountId
            loadAccount()
            loadTransactions()
            fetchTransactions(false)
            fetchRate()
            initialized = true
        }
    }

    override fun onCleared() {
        transactionsLiveData.removeObserver(transactionsObserver)
    }

    fun fetchTransactions(showProgress: Boolean = true) {
        viewModelScope.launch {
            if (showProgress) refreshing.value = true
            try {
                transactionRepository.refresh(accountId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (showProgress) refreshing.value = false
        }
    }

    fun removeAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            database.accountDao().deleteById(accountId)
        }
    }

    private fun loadAccount() {
        viewModelScope.launch {
            account.value = async(Dispatchers.Default) {
                database.accountDao().getById(accountId)
            }.await()
        }
    }

    private fun loadTransactions() {
        transactionsLiveData.observeForever(transactionsObserver)
    }

    private fun fetchRate() = viewModelScope.launch {
        try {
            prefs.rate = coinMarketCapClient.fetchRate(prefs.currencyCode).toFloat()
            updateItems()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun updateItems() {
        val items = mutableListOf<Item>()

        val rate = prefs.rate.toDouble()
        val currencyCode = prefs.currencyCode

        items.add(AccountSummaryItem(summary, rate, currencyCode))

        var lastDate: String? = null

        for (transaction in transactions) {
            val date = transaction.tx.getBlockDateFormatted() ?: ""

            if (lastDate != date) {
                items.add(DateItem(transaction.tx.getBlockDate()))
            }

            lastDate = date

            items.add(TransactionItem(transaction, rate, currencyCode))
        }

        this.items.value = items
        this.empty.value = transactions.isEmpty()
    }
}