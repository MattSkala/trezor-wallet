package cz.skala.trezorwallet.discovery

import android.util.Log
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.insight.response.Tx
import java.util.*

/**
 * A helper class for fetching transactions from Insight API.
 */
class TransactionFetcher {
    companion object {
        private const val GAP_SIZE = 20
        private const val TAG = "TransactionFetcher"
    }

    lateinit var insightApi: InsightApiService

    /**
     * Fetches transactions for all active addresses in an account.
     */
    fun fetchTransactionsForAccount(accountNode: ExtendedPublicKey): Pair<Set<Tx>, List<String>> {
        val externalChainNode = accountNode.deriveChildKey(0)
        val changeNode = accountNode.deriveChildKey(1)

        val txs = mutableSetOf<Tx>()
        val addresses = mutableListOf<String>()

        Log.d(TAG, "fetchTransactionsForChainNode 0")
        txs += fetchTransactionsForChainNode(externalChainNode, addresses)

        Log.d(TAG, "fetchTransactionsForChainNode 1")
        txs += fetchTransactionsForChainNode(changeNode, addresses)

        return Pair(txs, addresses)
    }

    private fun fetchTransactionsForChainNode(externalChainNode: ExtendedPublicKey, addresses: MutableList<String>): List<Tx> {
        var from = 0
        val txs = mutableListOf<Tx>()
        do {
            val addrs = mutableListOf<String>()
            for (addressIndex in from until from + GAP_SIZE) {
                val addressNode = externalChainNode.deriveChildKey(addressIndex)
                val address = addressNode.getAddress()
                addrs.add(address)
                Log.d(TAG, "/$addressIndex $address")
            }
            val page = fetchTransactions(addrs)
            addresses += addrs
            txs += page
            from += GAP_SIZE
        } while (page.isNotEmpty())
        return txs
    }

    private fun fetchTransactions(addresses: List<String>): List<Tx> {
        val response = insightApi.getAddrsTxs(addresses.joinToString(","), 0, 50).execute()
        // TODO: add pagination
        val body = response.body()
        if (response.isSuccessful && body != null) {
            return body.items
        } else {
            throw Exception("An error occured while fetching transactions")
        }
    }

    fun calculateBalance(txs: Set<Tx>, addresses: List<String>): Double {
        var received = 0.0
        var sent = 0.0

        txs.forEach { tx ->
            Log.d(TAG, "tx " + tx.txid + " " + Date(tx.blocktime * 1000))
            val isOutgoing = tx.vin.all { addresses.contains(it.addr) }

            tx.vout.forEach { txOut ->
                txOut.scriptPubKey.addresses?.forEach { addr ->
                    if (isOutgoing) {
                        if (!addresses.contains(addr)) {
                            Log.d(TAG, "addr " + addr + " -" + txOut.value)
                            sent += txOut.value.toDouble()
                        }
                    } else {
                        if (addresses.contains(addr)) {
                            received += txOut.value.toDouble()
                            Log.d(TAG, "addr " + addr + " +" + txOut.value)
                        }
                    }

                }
            }

            if (isOutgoing) {
                sent += tx.fees
            }
        }

        val balance = received - sent

        Log.d(TAG, "total txs:" + txs.size)
        Log.d(TAG, "total addresses:" + addresses.size)
        Log.d(TAG, "received:" + received)
        Log.d(TAG, "sent:" + sent)
        Log.d(TAG, "balance:" + balance)

        return balance
    }
}