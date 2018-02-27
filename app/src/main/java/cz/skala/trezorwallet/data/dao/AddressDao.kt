package cz.skala.trezorwallet.data.dao

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
    @Query("SELECT * FROM addresses WHERE account = :account AND change = :change")
    fun getByAccount(account: Int, change: Boolean): List<Address>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(address: Address)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(addresses: List<Address>)

    @Query("DELETE FROM addresses")
    fun deleteAll()
}