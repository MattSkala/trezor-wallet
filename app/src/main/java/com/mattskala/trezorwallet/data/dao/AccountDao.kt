package com.mattskala.trezorwallet.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.mattskala.trezorwallet.data.entity.Account

/**
 * An account DAO.
 */
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY `legacy`, `index` ASC")
    fun getAll(): List<Account>

    @Query("SELECT * FROM accounts ORDER BY `legacy`, `index` ASC")
    fun getAllLiveData(): LiveData<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getById(id: String): Account

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(account: Account)

    @Query("UPDATE accounts SET balance = :balance WHERE id = :id")
    fun updateBalance(id: String, balance: Long)

    @Query("UPDATE accounts SET label = null")
    fun clearLabels()

    @Query("DELETE FROM accounts")
    fun deleteAll()

    @Query("DELETE FROM accounts WHERE id = :id")
    fun deleteById(id: String)
}