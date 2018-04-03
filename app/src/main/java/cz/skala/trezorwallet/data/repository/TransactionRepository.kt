package cz.skala.trezorwallet.data.repository

import android.arch.lifecycle.LiveData
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.*
import cz.skala.trezorwallet.discovery.TransactionFetcher
import cz.skala.trezorwallet.insight.response.Tx
import cz.skala.trezorwallet.ui.btcToSat

class TransactionRepository(
        val database: AppDatabase,
        val fetcher: TransactionFetcher) {

    /**
     * Gets observable transactions list for a specific account.
     */
    fun getByAccount(accountId: String): LiveData<List<TransactionWithInOut>> {
        return database.transactionDao().getByAccountLiveDataWithInOut(accountId)
    }

    /**
     * Fetches the transactions list from Insight API and updates the local database.
     */
    fun refresh(accountId: String) {
        val account = database.accountDao().getById(accountId)
        val (txs, externalChainAddresses, changeAddresses) =
                fetcher.fetchTransactionsForAccount(account)

        val myAddresses = externalChainAddresses + changeAddresses
        val transactions = createTransactionEntities(txs, accountId, myAddresses, changeAddresses)

        saveTransactions(transactions)

        val externalChainAddressEntities = createAddressEntities(accountId, externalChainAddresses, false)
        calculateAddressTotalReceived(txs, externalChainAddressEntities)
        saveAddresses(externalChainAddressEntities)
        saveAddresses(createAddressEntities(accountId, changeAddresses, true))

        val balance = fetcher.calculateBalance(txs, myAddresses)
        database.accountDao().updateBalance(accountId, balance)
    }

    private fun createAddressEntities(accountId: String, addrs: List<String>, change: Boolean): List<Address> {
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

    private fun saveAddresses(addresses: List<Address>) {
        database.addressDao().insert(addresses)
    }

    private fun createTransactionEntities(txs: Set<Tx>, accountId: String, addresses: List<String>,
                                          changeAddresses: List<String>): List<TransactionWithInOut> {
        return txs.map {
            createTransactionEntity(it, accountId, addresses, changeAddresses)
        }
    }

    private fun createTransactionEntity(tx: Tx, accountId: String, myAddresses: List<String>,
                                        changeAddresses: List<String>): TransactionWithInOut {
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

        var value = 0L
        tx.vout.forEach { txOut ->
            txOut.scriptPubKey.addresses?.forEach { addr ->
                if (isSent) {
                    if (!myAddresses.contains(addr)) {
                        value += btcToSat(txOut.value.toDouble())
                    }
                } else if (isReceived) {
                    if (myAddresses.contains(addr)) {
                        value += btcToSat(txOut.value.toDouble())
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
                btcToSat(tx.fees),
                tx.locktime
        )

        val vin = tx.vin.map {
            TransactionInput(accountTxid, accountId, it.n, it.txid, it.vout, it.addr, it.valueSat,
                    it.scriptSig.hex, it.sequence)
        }

        val vout = tx.vout.map {
            val myAddress = it.scriptPubKey.addresses?.find { myAddresses.contains(it) }
            val isMine = myAddress != null
            val address = myAddress ?: it.scriptPubKey.addresses?.get(0)
            val isChange = changeAddresses.contains(address)

            TransactionOutput(accountTxid, accountId, tx.txid, it.n, address,
                    btcToSat(it.value.toDouble()), it.spentTxId, isMine, isChange, it.scriptPubKey.hex)
        }

        return TransactionWithInOut(transaction, vin, vout)
    }

    private fun saveTransactions(transactions: List<TransactionWithInOut>) {
        database.transactionDao().insertTransactions(transactions)
    }
}