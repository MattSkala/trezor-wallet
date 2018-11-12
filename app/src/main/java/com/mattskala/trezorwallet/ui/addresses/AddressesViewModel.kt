package com.mattskala.trezorwallet.ui.addresses

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.mattskala.trezorwallet.R
import com.mattskala.trezorwallet.data.AppDatabase
import com.mattskala.trezorwallet.data.entity.Address
import com.mattskala.trezorwallet.data.item.AddressItem
import com.mattskala.trezorwallet.data.item.ButtonItem
import com.mattskala.trezorwallet.data.item.Item
import com.mattskala.trezorwallet.data.item.SectionItem
import com.mattskala.trezorwallet.ui.BaseViewModel
import org.kodein.di.generic.instance

/**
 * A ViewModel for AddressesFragment.
 */
class AddressesViewModel(app: Application) : BaseViewModel(app) {
    companion object {
        private const val FRESH_ADDRESSES_LIMIT = 20
    }

    private val database: AppDatabase by instance()

    val items = MutableLiveData<List<Item>>()

    private var initialized = false
    private lateinit var accountId: String

    private var addresses: List<Address> = mutableListOf()
    private var previousAddressesExpanded = false
    private var freshAddressesCount = 1

    private val addressesLiveData by lazy {
        database.addressDao().getByAccountLiveData(accountId, false)
    }

    private val addressesObserver = Observer<List<Address>> {
        if (it != null) {
            addresses = it
            updateItems()
        }
    }

    fun start(accountId: String) {
        if (!initialized) {
            this.accountId = accountId
            addressesLiveData.observeForever(addressesObserver)
            initialized = true
        }
    }

    fun togglePreviousAddresses() {
        previousAddressesExpanded = !previousAddressesExpanded
        updateItems()
    }

    fun addFreshAddress() {
        freshAddressesCount++
        updateItems()
    }

    override fun onCleared() {
        addressesLiveData.removeObserver(addressesObserver)
    }

    private fun updateItems() {
        val items = mutableListOf<Item>()

        val firstFreshIndex = findFirstFreshIndex(addresses)

        items.add(SectionItem(R.string.previous_addresses, true, previousAddressesExpanded))

        addresses.forEachIndexed { index, address ->
            if (index == firstFreshIndex) {
                items.add(SectionItem(R.string.fresh_address))
            }

            if ((index < firstFreshIndex && previousAddressesExpanded) ||
                    (index >= firstFreshIndex && index < firstFreshIndex + freshAddressesCount)) {
                items.add(AddressItem(address))
            }
        }

        if (freshAddressesCount < FRESH_ADDRESSES_LIMIT) {
            items.add(ButtonItem(R.string.more_addresses))
        }

        this.items.value = items
    }

    private fun findFirstFreshIndex(addresses: List<Address>): Int {
        var lastUsedIndex: Int = -1
        addresses.forEachIndexed { index, address ->
            if (address.totalReceived > 0) {
                lastUsedIndex = index
            }
        }
        return lastUsedIndex + 1
    }
}