package com.mattskala.trezorwallet.discovery

import com.mattskala.trezorwallet.blockbook.response.Tx
import com.mattskala.trezorwallet.data.entity.Address
import com.mattskala.trezorwallet.data.entity.Transaction
import com.mattskala.trezorwallet.data.entity.TransactionWithInOut
import com.mattskala.trezorwallet.ui.transactions.AccountSummary

class BalanceCalculator {
    /**
     * Calculates the total received amount for each of the provided addresses.
     */
    fun calculateAddressTotalReceived(txs: Set<Tx>, addresses: List<Address>) {
        txs.forEach { tx ->
            tx.outputs.forEach { txOut ->
                val address = addresses.find { it.address == txOut.address }
                if (address != null) {
                    address.totalReceived += txOut.satoshis
                }
            }
        }
    }

    /**
     * Calculates the sum of received and sent transaction value.
     */
    fun createAccountSummary(transactions: List<TransactionWithInOut>): AccountSummary {
        var received = 0L
        var sent = 0L
        transactions.forEach {
            when (it.tx.type) {
                Transaction.Type.RECV -> received += it.tx.value
                Transaction.Type.SENT -> sent += it.tx.value + it.tx.fee
                Transaction.Type.SELF -> sent += it.tx.fee
            }
        }
        return AccountSummary(received, sent)
    }
}