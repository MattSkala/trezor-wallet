package cz.skala.trezorwallet.discovery

import cz.skala.trezorwallet.blockbook.BlockbookSocketService
import cz.skala.trezorwallet.blockbook.options.GetAddressHistoryOptions
import cz.skala.trezorwallet.blockbook.response.Tx
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.Account
import kotlinx.coroutines.runBlocking

/**
 * A helper class for fetching transactions from Insight API.
 */
class TransactionFetcher(
        val blockbookSocketService: BlockbookSocketService,
        val prefs: PreferenceHelper
) {
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

    private fun fetchTransactionsForChainNode(externalChainNode: ExtendedPublicKey,
                                              addresses: MutableList<String>, legacy: Boolean): List<Tx> {
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

    fun fetchTransactions(addresses: List<String>, to: Int = Int.MAX_VALUE): List<Tx> = runBlocking {
        val confirmed = fetchTransactions(addresses, to, false)
        val unconfirmed = fetchTransactions(addresses, to, true)
        confirmed + unconfirmed
    }

    private suspend fun fetchTransactions(addresses: List<String>, to: Int, mempool: Boolean): List<Tx> {
        val options = GetAddressHistoryOptions(start = prefs.blockHeight, end = prefs.syncHeight,
                to = to, queryMempoolOnly = mempool)
        val result = blockbookSocketService.getAddressHistory(addresses, options)
        return result.items.map { it.tx }
    }
}