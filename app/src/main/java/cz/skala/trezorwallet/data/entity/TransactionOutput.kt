package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity

/**
 * A transaction output entity.
 */
@Entity(tableName = "transaction_outputs", primaryKeys = ["accountTxid", "n"])
class TransactionOutput(
        val accountTxid: String,
        val account: String,
        val txid: String,
        val n: Int,
        val addr: String?,
        val value: Long,
        val spentTxId: String?,
        val isMine: Boolean = false,
        val isChange: Boolean = false,
        val scriptPubKey: String
)