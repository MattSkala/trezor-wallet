package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * A transaction entity.
 */
@Entity(tableName = "transactions")
class Transaction(
        @PrimaryKey val txid: String,
        val account: Int,
        val version: Int,
        val time: Long,
        val size: Int,
        val blockheight: Long,
        val blockhash: String,
        val blocktime: Long,
        val confirmations: Int
)