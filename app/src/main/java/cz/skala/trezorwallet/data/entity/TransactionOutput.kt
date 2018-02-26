package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity

/**
 * A transaction output entity.
 */
@Entity(tableName = "transaction_outputs", primaryKeys = ["txid", "n"])
class TransactionOutput(
        val txid: String,
        val n: Int,
        val addr: String?,
        val value: Double,
        val spentTxId: String?
)