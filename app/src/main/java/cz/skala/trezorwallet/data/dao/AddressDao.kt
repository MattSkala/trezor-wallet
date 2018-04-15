package cz.skala.trezorwallet.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import cz.skala.trezorwallet.data.entity.Address

/**
 * An address DAO.
 */
@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses WHERE account = :account AND address = :address")
    fun getByAddress(account: String, address: String): Address

    @Query("SELECT * FROM addresses WHERE account = :account AND change = :change ORDER BY `index` ASC")
    fun getByAccount(account: String, change: Boolean): List<Address>

    @Query("SELECT * FROM addresses WHERE account = :account AND change = :change ORDER BY `index` ASC")
    fun getByAccountLiveData(account: String, change: Boolean): LiveData<List<Address>>

    @Query("UPDATE addresses SET label = null")
    fun clearLabels()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(address: Address)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(addresses: List<Address>)

    @Query("DELETE FROM addresses")
    fun deleteAll()
}