package cz.skala.trezorwallet.ui.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Transaction
import cz.skala.trezorwallet.data.entity.TransactionInput
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.discovery.TransactionFetcher
import cz.skala.trezorwallet.insight.response.Tx
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * A ViewModel for TransactionsFragment.
 */
class TransactionsViewModel(
        val database: AppDatabase,
        val fetcher: TransactionFetcher
) : ViewModel() {
    val transactions = MutableLiveData<List<TransactionWithInOut>>()
    val refreshing = MutableLiveData<Boolean>()

    var initialized = false
    lateinit var accountId: String

    fun start(accountId: String) {
        if (!initialized) {
            this.accountId = accountId
            fetchTransactions()
            initialized = true
        }
    }

    fun fetchTransactions() {
        async(UI) {
            refreshing.value = true
            val txs = bg {
                val account = database.accountDao().getById(accountId)
                val publicKey = account.publicKey
                val chainCode = account.chainCode
                val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(publicKey), chainCode)

                val (txs, addresses) = fetcher.fetchTransactionsForAccount(accountNode)

                val transactions = createTransactionEntities(txs, accountId, addresses)

                fetcher.calculateBalance(txs, addresses)

                transactions
            }.await()
            refreshing.value = false
            transactions.value = txs
        }
    }

    private fun createTransactionEntities(txs: Set<Tx>, accountId: String, addresses: List<String>): List<TransactionWithInOut> {
        return txs.map {
            createTransactionEntity(it, accountId, addresses)
        }.sortedWith(compareBy({ it.tx.blockheight != null }, { it.tx.blockheight })).reversed()
    }

    private fun createTransactionEntity(tx: Tx, accountId: String, myAddresses: List<String>): TransactionWithInOut {
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

        val transaction = Transaction(
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
            TransactionInput(tx.txid, it.n, it.addr, it.value)
        }

        val vout = tx.vout.map {
            TransactionOutput(tx.txid, it.n, it.scriptPubKey.addresses?.get(0),
                    it.value.toDouble(), it.spentTxId)
        }

        return TransactionWithInOut(transaction, vin, vout)
    }

    class Factory(val database: AppDatabase, val fetcher: TransactionFetcher) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TransactionsViewModel(database, fetcher) as T
        }
    }
}