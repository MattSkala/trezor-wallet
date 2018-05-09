package cz.skala.trezorwallet.discovery

import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.data.entity.Account
import cz.skala.trezorwallet.insight.InsightApiService
import cz.skala.trezorwallet.insight.response.Tx

/**
 * A helper class for fetching transactions from Insight API.
 */
class TransactionFetcher(val insightApi: InsightApiService) {
    companion object {
        private const val GAP_SIZE = 20
        private const val PAGE_SIZE = 50
    }

    /**
     * Fetches transactions for all active addresses in an account.
     */
    fun fetchTransactionsForAccount(account: Account): Triple<Set<Tx>, List<String>, List<String>> {
        val publicKey = account.publicKey
        val chainCode = account.chainCode
        val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(publicKey), chainCode)

        val externalChainNode = accountNode.deriveChildKey(0)
        val changeNode = accountNode.deriveChildKey(1)

        val txs = mutableSetOf<Tx>()
        val externalChainAddresses = mutableListOf<String>()
        val changeAddresses = mutableListOf<String>()

        txs += fetchTransactionsForChainNode(externalChainNode, externalChainAddresses, account.legacy)

        txs += fetchTransactionsForChainNode(changeNode, changeAddresses, account.legacy)

        return Triple(txs, externalChainAddresses, changeAddresses)
    }

    private fun fetchTransactionsForChainNode(externalChainNode: ExtendedPublicKey, addresses: MutableList<String>, legacy: Boolean): List<Tx> {
        var from = 0
        val txs = mutableListOf<Tx>()
        do {
            val addrs = mutableListOf<String>()
            for (addressIndex in from until from + GAP_SIZE) {
                val addressNode = externalChainNode.deriveChildKey(addressIndex)
                val address = if (legacy) addressNode.getAddress() else addressNode.getSegwitAddress()
                addrs.add(address)
            }
            val page = fetchTransactions(addrs)
            addresses += addrs
            txs += page
            from += GAP_SIZE
        } while (page.isNotEmpty())
        return txs
    }

    private fun fetchTransactions(addresses: List<String>): List<Tx> {
        val txs = mutableListOf<Tx>()
        var from = 0
        do {
            val page = fetchTransactionsPage(addresses, from, from + PAGE_SIZE)
            txs += page
            from += PAGE_SIZE
        } while (page.isNotEmpty())
        return txs
    }

    fun fetchTransactionsPage(addresses: List<String>, from: Int, to: Int): List<Tx> {
        val response = insightApi.getAddrsTxs(addresses.joinToString(","), from, to).execute()
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
            val isOutgoing = tx.vin.all { addresses.contains(it.addr) }

            tx.vout.forEach { txOut ->
                txOut.scriptPubKey.addresses?.forEach { addr ->
                    if (isOutgoing) {
                        if (!addresses.contains(addr)) {
                            sent += txOut.value.toDouble()
                        }
                    } else {
                        if (addresses.contains(addr)) {
                            received += txOut.value.toDouble()
                        }
                    }

                }
            }

            if (isOutgoing) {
                sent += tx.fees
            }
        }

        return received - sent
    }
}