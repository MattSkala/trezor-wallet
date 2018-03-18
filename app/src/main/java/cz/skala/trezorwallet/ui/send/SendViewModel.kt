package cz.skala.trezorwallet.ui.send

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import cz.skala.trezorwallet.data.AppDatabase

/**
 * A ViewModel for SendFragment.
 */
class SendViewModel(val database: AppDatabase) : ViewModel() {

    class Factory(val database: AppDatabase) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SendViewModel(database) as T
        }
    }
}