package cz.skala.trezorwallet.data.item

import cz.skala.trezorwallet.ui.transactions.AccountSummary

class AccountSummaryItem(val summary: AccountSummary, val rate: Double, val currencyCode: String) : Item()