package cz.skala.trezorwallet.data.repository

import android.arch.lifecycle.LiveData
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountRepository(
        private val database: AppDatabase
) {
    suspend fun getById(accountId: String) = withContext(Dispatchers.IO) {
        database.accountDao().getById(accountId)
    }

    suspend fun getAll(): List<Account> = withContext(Dispatchers.IO) {
        database.accountDao().getAll()
    }

    suspend fun insert(account: Account) = withContext(Dispatchers.IO) {
        database.accountDao().insert(account)
    }

    fun getAllLiveData(): LiveData<List<Account>> =
            database.accountDao().getAllLiveData()
}