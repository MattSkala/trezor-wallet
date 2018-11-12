package com.mattskala.trezorwallet.data.item

import com.mattskala.trezorwallet.ui.transactions.AccountSummary

class AccountSummaryItem(val summary: AccountSummary, val rate: Double, val currencyCode: String) : Item()