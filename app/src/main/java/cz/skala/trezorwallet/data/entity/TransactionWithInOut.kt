package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation

/**
 * A transaction with inputs and outputs.
 */
class TransactionWithInOut(
        @Embedded val tx: Transaction,

        @Relation(parentColumn = "txid", entityColumn = "txid")
        val vin: List<TransactionInput>,

        @Relation(parentColumn = "txid", entityColumn = "txid")
        val vout: List<TransactionOutput>
)