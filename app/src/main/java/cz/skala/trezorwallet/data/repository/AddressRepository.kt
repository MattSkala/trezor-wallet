package cz.skala.trezorwallet.data.repository

import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddressRepository(
        private val database: AppDatabase
) {
    suspend fun getByAddress(address: String) = withContext(Dispatchers.IO) {
        database.addressDao().getByAddress(address)
    }

    suspend fun insert(address: Address) = withContext(Dispatchers.IO) {
        database.addressDao().insert(address)
    }
}