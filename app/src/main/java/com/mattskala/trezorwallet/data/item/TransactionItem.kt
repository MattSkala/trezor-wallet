package com.mattskala.trezorwallet.data.item

import com.mattskala.trezorwallet.data.entity.TransactionWithInOut


class TransactionItem(val transaction: TransactionWithInOut, val rate: Double, val currencyCode: String) : Item()