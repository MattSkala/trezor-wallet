package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity

/**
 * A transaction output entity.
 */
@Entity(tableName = "transaction_outputs", primaryKeys = ["txid", "account", "n"])
class TransactionOutput(
        val accountTxid: String,
        val txid: String,
        val account: String,
        val n: Int,
        val addr: String?,
        val value: Double,
        val spentTxId: String?,
        val isMine: Boolean = false,
        val isChange: Boolean = false
)