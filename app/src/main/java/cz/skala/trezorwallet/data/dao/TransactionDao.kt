package cz.skala.trezorwallet.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import cz.skala.trezorwallet.data.entity.Transaction
import cz.skala.trezorwallet.data.entity.TransactionInput
import cz.skala.trezorwallet.data.entity.TransactionOutput
import cz.skala.trezorwallet.data.entity.TransactionWithInOut

/**
 * A transaction DAO.
 */
@Dao
abstract class TransactionDao {
    @Query("SELECT * FROM transactions WHERE txid = :txid")
    @android.arch.persistence.room.Transaction
    abstract fun getByTxid(txid: String): TransactionWithInOut

    @Query("SELECT * FROM transactions WHERE account = :account")
    abstract fun getByAccount(account: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE account = :account")
    abstract fun getByAccountLiveData(account: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE account = :account")
    @android.arch.persistence.room.Transaction
    abstract fun getByAccountLiveDataWithInOut(account: String): LiveData<List<TransactionWithInOut>>

    @Query("SELECT * FROM transaction_outputs WHERE account = :account AND isMine = 1 " +
            "AND spentTxId IS NULL")
    abstract fun getUnspentOutputs(account: String): List<TransactionOutput>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(input: TransactionInput)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(output: TransactionOutput)

    @android.arch.persistence.room.Transaction
    open fun insert(transaction: TransactionWithInOut) {
        insert(transaction.tx)
        transaction.vin.forEach {
            insert(it)
        }
        transaction.vout.forEach {
            insert(it)
        }
    }

    @android.arch.persistence.room.Transaction
    open fun insertTransactions(transactions: List<TransactionWithInOut>) {
        transactions.forEach {
            insert(it)
        }
    }

    @Query("DELETE FROM transactions")
    abstract fun deleteAll()
}