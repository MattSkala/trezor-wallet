package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Entity
import java.text.SimpleDateFormat
import java.util.*

/**
 * A transaction entity.
 */
@Entity(tableName = "transactions", primaryKeys = ["txid", "account"])
class Transaction(
        /**
         * Account node hash combined with the transaction hash. It is required for establishing
         * correct relation with inputs and outputs in [TransactionWithInOut], as a transaction
         * can occur on multiple accounts in different directions.
         */
        val accountTxid: String,

        /**
         * Transaction hash.
         */
        val txid: String,

        /**
         * Account node hash.
         */
        val account: String,

        /**
         * Transaction format version.
         */
        val version: Int,

        /**
         * Size of the raw transaction in bytes.
         */
        val size: Int,

        /**
         * Height of the block in which the tx is included, -1 for unconfirmed txs.
         */
        val blockheight: Int,

        /**
         * Timestamp of the block in millis, null for unconfirmed tx.
         */
        val blocktime: Long?,

        /**
         * Transaction direction.
         */
        val type: Type,

        /**
         * Transaction value in satoshis.
         */
        val value: Long,

        /**
         * Transaction fee in satoshis.
         */
        val fee: Long,

        /**
         * The earliest block the transaction may be added to the blockchain.
         */
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