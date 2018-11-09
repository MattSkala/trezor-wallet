package cz.skala.trezorwallet.data.repository

import android.arch.lifecycle.LiveData
import android.util.Log
import cz.skala.trezorwallet.blockbook.response.Tx
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.*
import cz.skala.trezorwallet.discovery.BalanceCalculator
import cz.skala.trezorwallet.discovery.TransactionFetcher
import cz.skala.trezorwallet.labeling.AccountMetadata
import cz.skala.trezorwallet.labeling.LabelingManager

class TransactionRepository(
        val database: AppDatabase,
        val fetcher: TransactionFetcher,
        val labeling: LabelingManager,
        val balanceCalculator: BalanceCalculator) {

    /**
     * Gets observable transactions list for a specific account.
     */
    fun getByAccount(accountId: String): LiveData<List<TransactionWithInOut>> {
        return database.transactionDao().getByAccountLiveDataWithInOut(accountId)
    }

    /**
     * Fetches the transactions list and updates the local database.
     */
    fun refresh(accountId: String) {
        val account = database.accountDao().getById(accountId)
        val (txs, externalChainAddresses, changeAddresses) =
                fetcher.fetchTransactionsForAccount(account)

        val metadata = if (labeling.isEnabled()) {
            labeling.downloadAccountMetadata(account)
            labeling.loadMetadata(account)
        } else {
            null
        }

        val myAddresses = externalChainAddresses + changeAddresses
        val transactions = createTransactionEntities(txs, accountId,
                myAddresses, changeAddresses, metadata)

        saveTransactions(transactions)

        val externalChainAddressEntities = createAddressEntities(accountId,
                externalChainAddresses, false, metadata)
        balanceCalculator.calculateAddressTotalReceived(txs, externalChainAddressEntities)
        saveAddresses(externalChainAddressEntities)
        saveAddresses(createAddressEntities(accountId, changeAddresses, true, metadata))

        val uniqueTransactions = transactions.toSet().toList()
        val summary = balanceCalculator.createAccountSummary(uniqueTransactions)

        database.accountDao().updateBalance(accountId, summary.balance)
    }

    /**
     * Saves a tx to the database.
     */
    fun saveTx(tx: Tx, accountId: String) {
        val account = database.accountDao().getById(accountId)

        val externalChainAddresses = database.addressDao()
                .getByAccount(account.id, false)
                .map { it.address }
        val changeAddresses = database.addressDao()
                .getByAccount(account.id, true)
                .map { it.address }
        val myAddresses = externalChainAddresses + changeAddresses

        val transaction = TransactionWithInOut.create(tx, accountId,
                myAddresses, changeAddresses, null)
        database.transactionDao().insert(transaction)
    }

    private fun createAddressEntities(accountId: String, addrs: List<String>, change: Boolean,
                                      metadata: AccountMetadata?): List<Address> {
        return addrs.mapIndexed { index, addr ->
            Address(addr, accountId, change, index, metadata?.getAddressLabel(addr), 0L)
        }
    }

    private fun saveAddresses(addresses: List<Address>) {
        database.addressDao().insert(addresses)
    }

    private fun createTransactionEntities(txs: Set<Tx>, accountId: String, addresses: List<String>,
                                          changeAddresses: List<String>,
                                          metadata: AccountMetadata?): List<TransactionWithInOut> {
        return txs.map {
            TransactionWithInOut.create(it, accountId, addresses, changeAddresses, metadata)
        }
    }

    private fun saveTransactions(transactions: List<TransactionWithInOut>) {
        database.transactionDao().insertTransactions(transactions)
    }
}