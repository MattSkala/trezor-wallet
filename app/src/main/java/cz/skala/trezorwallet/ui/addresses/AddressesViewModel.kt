package cz.skala.trezorwallet.ui.addresses

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.AppDatabase
import cz.skala.trezorwallet.data.entity.Address
import cz.skala.trezorwallet.data.item.AddressItem
import cz.skala.trezorwallet.data.item.ButtonItem
import cz.skala.trezorwallet.data.item.Item
import cz.skala.trezorwallet.data.item.SectionItem

/**
 * A ViewModel for AddressesFragment.
 */
class AddressesViewModel(val database: AppDatabase) : ViewModel() {
    companion object {
        private const val FRESH_ADDRESSES_LIMIT = 20
    }

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

    class Factory(val database: AppDatabase) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return AddressesViewModel(database) as T
        }
    }
}