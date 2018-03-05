package cz.skala.trezorwallet.data.item

import cz.skala.trezorwallet.data.entity.TransactionWithInOut


class TransactionItem(val transaction: TransactionWithInOut, val rate: Double, val currencyCode: String) : Item()