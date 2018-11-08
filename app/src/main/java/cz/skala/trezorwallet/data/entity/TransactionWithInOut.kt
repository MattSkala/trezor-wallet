package cz.skala.trezorwallet.data.entity

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation
import cz.skala.trezorwallet.blockbook.response.Tx
import cz.skala.trezorwallet.labeling.AccountMetadata

/**
 * A transaction with inputs and outputs.
 */
class TransactionWithInOut() {
    companion object {
        fun create(tx: Tx, accountId: String, myAddresses: List<String>,
                           changeAddresses: List<String>, metadata: AccountMetadata?): TransactionWithInOut {
            val isSent = tx.inputs.all {
                myAddresses.contains(it.address)
            }

            val isReceived = tx.outputs.any {
                myAddresses.contains(it.address)
            }

            val isSelf = isSent && tx.outputs.all {
                myAddresses.contains(it.address)
            }

            val type = when {
                isSelf -> Transaction.Type.SELF
                isSent -> Transaction.Type.SENT
                else -> Transaction.Type.RECV
            }

            var value = 0L
            tx.outputs.forEach { txOut ->
                if (isSent) {
                    if (!myAddresses.contains(txOut.address)) {
                        value += txOut.satoshis
                    }
                } else if (isReceived) {
                    if (myAddresses.contains(txOut.address)) {
                        value += txOut.satoshis
                    }
                }
            }

            val accountTxid = accountId + "_" + tx.hash

            val transaction = Transaction(
                    accountTxid,
                    tx.hash,
                    accountId,
                    tx.version,
                    tx.hex.length / 2,
                    tx.height,
                    tx.blockTimestamp,
                    type,
                    value,
                    tx.feeSatoshis,
                    tx.locktime
            )

            val vin = tx.inputs.mapIndexed { index, txin ->
                TransactionInput(accountTxid, accountId, index, txin.txid, txin.outputIndex,
                        txin.address, txin.satoshis, txin.script, txin.sequence)
            }

            val vout = tx.outputs.mapIndexed { index, txout ->
                val isMine = myAddresses.contains(txout.address)
                val isChange = changeAddresses.contains(txout.address)
                val label = metadata?.getOutputLabel(tx.hash, index)

                TransactionOutput(accountTxid, accountId, tx.hash, index, txout.address,
                        txout.satoshis, isMine, isChange, txout.script, label)
            }

            return TransactionWithInOut(transaction, vin, vout)
        }
    }

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

    override fun equals(other: Any?): Boolean {
        return other is TransactionWithInOut && other.tx.txid == tx.txid
                && other.tx.account == tx.account
    }

    override fun hashCode(): Int {
        var result = tx.txid.hashCode()
        result = 31 * result + tx.account.hashCode()
        return result
    }
}