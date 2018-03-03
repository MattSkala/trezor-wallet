package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation

/**
 * A transaction with inputs and outputs.
 */
class TransactionWithInOut() {
    constructor(tx: Transaction, vin: List<TransactionInput>, vout: List<TransactionOutput>) : this() {
        this.tx = tx
        this.vin = vin
        this.vout = vout
    }

    @Embedded lateinit var tx: Transaction

    @Relation(parentColumn = "accountTxid", entityColumn = "accountTxid")
    var vin: List<TransactionInput> = mutableListOf()

    @Relation(parentColumn = "accountTxid", entityColumn = "accountTxid")
    var vout: List<TransactionOutput> = mutableListOf()
}