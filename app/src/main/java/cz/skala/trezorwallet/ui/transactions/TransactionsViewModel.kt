package cz.skala.trezorwallet.ui.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import cz.skala.trezorwallet.coinmarketcap.CoinMarketCapClient
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.*
import cz.skala.trezorwallet.data.item.AccountSummaryItem
import cz.skala.trezorwallet.data.item.DateItem
import cz.skala.trezorwallet.data.item.Item
import cz.skala.trezorwallet.data.item.TransactionItem
import cz.skala.trezorwallet.discovery.TransactionFetcher
import cz.skala.trezorwallet.insight.response.Tx
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A ViewModel for TransactionsFragment.
 */
class TransactionsViewModel(
        private val database: AppDatabase,
        private val fetcher: TransactionFetcher,
        private val coinMarketCapClient: CoinMarketCapClient,
        private val prefs: PreferenceHelper
) : ViewModel() {
    val items = MutableLiveData<List<Item>>()
    val refreshing = MutableLiveData<Boolean>()
    val empty = MutableLiveData<Boolean>()

    private var initialized = false
    private lateinit var accountId: String
    private var transactions = listOf<TransactionWithInOut>()
    private var summary = AccountSummary(0.0, 0.0)

    private val transactionsLiveData by lazy {
        database.transactionDao().getByAccountLiveDataWithInOut(accountId)
    }

    private val transactionsObserver = Observer<List<TransactionWithInOut>> { txs ->
        if (txs != null) {
            transactions = txs.sortedWith(compareBy({ it.tx.blockheight == -1 }, { it.tx.blockheight })).reversed()
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
            bg {
                val account = database.accountDao().getById(accountId)
                val (txs, externalChainAddresses, changeAddresses) = fetcher.fetchTransactionsForAccount(account)

                val myAddresses = externalChainAddresses + changeAddresses
                val transactions = createTransactionEntities(txs, accountId, myAddresses, changeAddresses)

                saveTransactions(transactions)

                val externalChainAddressEntities = createAddressEntities(externalChainAddresses, false)
                calculateAddressTotalReceived(txs, externalChainAddressEntities)
                saveAddresses(externalChainAddressEntities)
                saveAddresses(createAddressEntities(changeAddresses, true))

                val balance = fetcher.calculateBalance(txs, myAddresses)
                database.accountDao().updateBalance(accountId, balance)

                transactions
            }.await()
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

    private fun fetchRate() {
        launch(UI) {
            prefs.rate = bg {
                coinMarketCapClient.fetchRate(prefs.currencyCode)
            }.await().toFloat()
            updateItems()
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

    private fun createTransactionEntities(txs: Set<Tx>, accountId: String, addresses: List<String>, changeAddresses: List<String>): List<TransactionWithInOut> {
        return txs.map {
            createTransactionEntity(it, accountId, addresses, changeAddresses)
        }
    }

    private fun createTransactionEntity(tx: Tx, accountId: String, myAddresses: List<String>, changeAddresses: List<String>): TransactionWithInOut {
        val isSent = tx.vin.all {
            myAddresses.contains(it.addr)
        }

        val isReceived = tx.vout.any { out ->
            var myAddressInOutput = false
            out.scriptPubKey.addresses?.forEach { addr ->
                if (myAddresses.contains(addr)) {
                    myAddressInOutput = true
                }
            }
            myAddressInOutput
        }

        val isSelf = isSent && tx.vout.all { out ->
            var myAddressInOutput = false
            out.scriptPubKey.addresses?.forEach { addr ->
                if (myAddresses.contains(addr)) {
                    myAddressInOutput = true
                }
            }
            myAddressInOutput
        }

        val type = when {
            isSelf -> Transaction.Type.SELF
            isSent -> Transaction.Type.SENT
            else -> Transaction.Type.RECV
        }

        var value = 0.0
        tx.vout.forEach { txOut ->
            txOut.scriptPubKey.addresses?.forEach { addr ->
                if (isSent) {
                    if (!myAddresses.contains(addr)) {
                        value += txOut.value.toDouble()
                    }
                } else if (isReceived) {
                    if (myAddresses.contains(addr)) {
                        value += txOut.value.toDouble()
                    }
                }
            }
        }

        val accountTxid = accountId + "_" + tx.txid

        val transaction = Transaction(
                accountTxid,
                tx.txid,
                accountId,
                tx.version,
                tx.time,
                tx.size,
                tx.blockheight,
                tx.blockhash,
                tx.blocktime,
                tx.confirmations,
                type,
                value,
                tx.fees
        )

        val vin = tx.vin.map {
            TransactionInput(accountTxid, tx.txid, accountId, it.n, it.addr, it.value)
        }

        val vout = tx.vout.map {
            val myAddress = it.scriptPubKey.addresses?.find { myAddresses.contains(it) }
            val isMine = myAddress != null
            val address = myAddress ?: it.scriptPubKey.addresses?.get(0)
            val isChange = changeAddresses.contains(address)
            TransactionOutput(accountTxid, tx.txid, accountId, it.n, address, it.value.toDouble(), it.spentTxId, isMine, isChange)
        }

        return TransactionWithInOut(transaction, vin, vout)
    }

    private fun saveTransactions(transactions: List<TransactionWithInOut>) {
        database.transactionDao().insertTransactions(transactions)
    }

    private fun createAddressEntities(addrs: List<String>, change: Boolean): List<Address> {
        return addrs.mapIndexed { index, addr ->
            Address(
                    addr,
                    accountId,
                    change,
                    index,
                    null,
                    0.0
            )
        }
    }

    private fun calculateAddressTotalReceived(txs: Set<Tx>, addresses: List<Address>) {
        txs.forEach { tx ->
            tx.vout.forEach { txOut ->
                txOut.scriptPubKey.addresses?.forEach { addr ->
                    val address = addresses.find { it.address == addr }
                    if (address != null) {
                        address.totalReceived += txOut.value.toDouble()
                    }
                }
            }
        }
    }

    private fun createAccountSummary(transactions: List<TransactionWithInOut>): AccountSummary {
        var received = 0.0
        var sent = 0.0
        transactions.forEach {
            when (it.tx.type) {
                Transaction.Type.RECV -> received += it.tx.value
                Transaction.Type.SENT -> sent += it.tx.value + it.tx.fee
                Transaction.Type.SELF -> sent += it.tx.fee
            }
        }
        return AccountSummary(received, sent)
    }

    private fun saveAddresses(addresses: List<Address>) {
        database.addressDao().insert(addresses)
    }

    class Factory(val database: AppDatabase, val fetcher: TransactionFetcher,
                  val coinMarketCapClient: CoinMarketCapClient, val prefs: PreferenceHelper
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TransactionsViewModel(database, fetcher, coinMarketCapClient, prefs) as T
        }
    }
}