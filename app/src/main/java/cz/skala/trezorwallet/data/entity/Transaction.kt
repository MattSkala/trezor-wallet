package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity
import android.content.res.Resources
import cz.skala.trezorwallet.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * A transaction entity.
 */
@Entity(tableName = "transactions", primaryKeys = ["txid", "account"])
class Transaction(
        val txid: String,
        val account: String,
        val version: Int,
        val time: Long,
        val size: Int,
        val blockheight: Int?,
        val blockhash: String?,
        val blocktime: Long?,
        val confirmations: Int,
        val type: Type,
        val value: Double,
        val fee: Double
) {
    enum class Type {
        SELF, RECV, SENT
    }

    fun getBlockTimeFormatted(resources: Resources): String {
        return if (blocktime != null) {
            val date = Date(blocktime * 1000)
            SimpleDateFormat.getDateTimeInstance().format(date)
        } else {
            resources.getString(R.string.tx_unconfirmed)
        }
    }
}