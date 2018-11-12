package com.mattskala.trezorwallet.data.entity

import android.arch.persistence.room.Entity
import android.content.res.Resources
import com.mattskala.trezorwallet.R

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
        val isMine: Boolean = false,
        val isChange: Boolean = false,
        val scriptPubKey: String,
        var label: String?
) {
    fun getDisplayLabel(resources: Resources): String {
        val label = label
        return when {
            label != null && label.isNotEmpty() -> label
            addr != null -> addr
            else -> resources.getString(R.string.unknown_address)
        }
    }
}