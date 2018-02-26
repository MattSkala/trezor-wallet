package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity

/**
 * A transaction input entity.
 */
@Entity(tableName = "transaction_inputs", primaryKeys = ["txid", "n"])
class TransactionInput(
        val txid: String,
        val n: Int,
        val addr: String,
        val value: Double
)