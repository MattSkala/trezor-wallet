package cz.skala.trezorwallet.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import cz.skala.trezorwallet.data.entity.Transaction

/**
 * A transaction DAO.
 */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE account = :account")
    fun getByAccount(account: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE account = :account")
    fun getByAccountLiveData(account: String): LiveData<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactions: List<Transaction>)

    @Query("DELETE FROM transactions")
    fun deleteAll()
}