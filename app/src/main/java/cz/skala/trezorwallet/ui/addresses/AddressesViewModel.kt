package cz.skala.trezorwallet.ui.addresses

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import cz.skala.trezorwallet.data.AppDatabase

/**
 * A ViewModel for AddressesFragment.
 */
class AddressesViewModel(val database: AppDatabase) : ViewModel() {
    val addresses by lazy {
        database.addressDao().getByAccountLiveData(accountId, false)
    }

    lateinit var accountId: String

    fun start(accountId: String) {
        this.accountId = accountId
    }

    class Factory(val database: AppDatabase) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return AddressesViewModel(database) as T
        }
    }
}