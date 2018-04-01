package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity
import java.text.SimpleDateFormat
import java.util.*

/**
 * A transaction entity.
 */
@Entity(tableName = "transactions", primaryKeys = ["txid", "account"])
class Transaction(
        val accountTxid: String,
        val txid: String,
        val account: String,
        val version: Int,
        val time: Long,
        val size: Int,
        val blockheight: Int,
        val blockhash: String?,
        val blocktime: Long?,
        val confirmations: Int,
        val type: Type,
        val value: Long,
        val fee: Long,
        val locktime: Int
) {
    enum class Type {
        SELF, RECV, SENT
    }

    fun getBlockDate(): Date? {
        return if (blocktime != null) Date(blocktime * 1000) else null
    }

    fun getBlockDateFormatted(): String? {
        return if (blocktime != null) {
            val date = Date(blocktime * 1000)
            SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG).format(date)
        } else {
            null
        }
    }

    fun getBlockTimeFormatted(): String? {
        return if (blocktime != null) {
            val date = Date(blocktime * 1000)
            SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(date)
        } else {
            null
        }
    }
}