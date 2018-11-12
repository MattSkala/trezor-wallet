package com.mattskala.trezorwallet.data.repository

import com.mattskala.trezorwallet.data.AppDatabase
import com.mattskala.trezorwallet.data.entity.Address
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