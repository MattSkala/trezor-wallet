package cz.skala.trezorwallet.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import cz.skala.trezorwallet.data.entity.Transaction

/**
 * A ViewModel for TransactionsFragment.
 */
class TransactionsViewModel : ViewModel() {
    val transactions = MutableLiveData<List<Transaction>>()
}