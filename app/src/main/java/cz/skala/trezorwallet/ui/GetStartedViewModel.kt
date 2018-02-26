package cz.skala.trezorwallet.ui

import android.arch.lifecycle.ViewModel
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager

/**
 * A ViewModel for GetStartedActivity.
 */
class GetStartedViewModel : ViewModel() {
    private lateinit var accountDiscovery: AccountDiscoveryManager

    fun startAccountDiscovery() {
        accountDiscovery.startAccountDiscovery()
    }
}