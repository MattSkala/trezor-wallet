package cz.skala.trezorwallet.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import cz.skala.trezorwallet.data.entity.Account

/**
 * An account DAO.
 */
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAll(): List<Account>

    @Query("SELECT * FROM accounts ORDER BY `legacy`, `index` ASC")
    fun getAllLiveData(): LiveData<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getById(id: String): Account

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(account: Account)

    @Query("DELETE FROM accounts")
    fun deleteAll()
}