package com.mattskala.trezorwallet.data.entity

import android.arch.persistence.room.Entity

/**
 * A transaction input entity.
 */
@Entity(tableName = "transaction_inputs", primaryKeys = ["accountTxid", "n"])
class TransactionInput(
        val accountTxid: String,
        val account: String,
        val n: Int,
        val txid: String,
        val vout: Int,
        val addr: String?,
        val value: Long,
        val scriptSig: String,
        val sequence: Long
)