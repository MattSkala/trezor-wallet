package cz.skala.trezorwallet.ui.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import cz.skala.trezorwallet.coinmarketcap.CoinMarketCapClient
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.Transaction
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.data.item.AccountSummaryItem
import cz.skala.trezorwallet.data.item.DateItem
import cz.skala.trezorwallet.data.item.Item
import cz.skala.trezorwallet.data.item.TransactionItem
import cz.skala.trezorwallet.data.repository.TransactionRepository
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A ViewModel for TransactionsFragment.
 */
class TransactionsViewModel(
        private val database: AppDatabase,
        private val coinMarketCapClient: CoinMarketCapClient,
        private val prefs: PreferenceHelper,
        private val transactionRepository: TransactionRepository
) : ViewModel() {
    val items = MutableLiveData<List<Item>>()
    val refreshing = MutableLiveData<Boolean>()
    val empty = MutableLiveData<Boolean>()

    private var initialized = false
    private lateinit var accountId: String
    private var transactions = listOf<TransactionWithInOut>()
    private var summary = AccountSummary(0L, 0L)

    private val transactionsLiveData by lazy {
        transactionRepository.getByAccount(accountId)
    }

    private val transactionsObserver = Observer<List<TransactionWithInOut>> { txs ->
        if (txs != null) {
            transactions = txs.sortedWith(compareBy({ it.tx.blockheight == -1 },
                    { it.tx.blockheight })).reversed()
            summary = createAccountSummary(txs)
            updateItems()
        }
    }

    fun start(accountId: String) {
        if (!initialized) {
            this.accountId = accountId
            loadTransactions()
            fetchTransactions()
            fetchRate()
            initialized = true
        }
    }

    override fun onCleared() {
        transactionsLiveData.removeObserver(transactionsObserver)
    }

    fun fetchTransactions() {
        launch(UI) {
            refreshing.value = true
            try {
                bg {
                    transactionRepository.refresh(accountId)
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            refreshing.value = false
        }
    }

    fun removeAccount() {
        launch(UI) {
            bg {
                database.accountDao().deleteById(accountId)
            }.await()
        }
    }

    private fun loadTransactions() {
        transactionsLiveData.observeForever(transactionsObserver)
    }

    private fun fetchRate() = launch(UI) {
        try {
            prefs.rate = coinMarketCapClient.fetchRate(prefs.currencyCode).toFloat()
            updateItems()
        } catch (e: Exception) {
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

    private fun createAccountSummary(transactions: List<TransactionWithInOut>): AccountSummary {
        var received = 0L
        var sent = 0L
        transactions.forEach {
            when (it.tx.type) {
                Transaction.Type.RECV -> received += it.tx.value
                Transaction.Type.SENT -> sent += it.tx.value + it.tx.fee
                Transaction.Type.SELF -> sent += it.tx.fee
            }
        }
        return AccountSummary(received, sent)
    }

    class Factory(val database: AppDatabase, val coinMarketCapClient: CoinMarketCapClient,
                  val prefs: PreferenceHelper, val transactionRepository: TransactionRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TransactionsViewModel(database, coinMarketCapClient, prefs, transactionRepository) as T
        }
    }
}