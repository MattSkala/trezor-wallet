package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity

/**
 * A transaction input entity.
 */
@Entity(tableName = "transaction_inputs", primaryKeys = ["txid", "account", "n"])
class TransactionInput(
        val accountTxid: String,
        val txid: String,
        val account: String,
        val n: Int,
        val addr: String,
        val value: Double
)