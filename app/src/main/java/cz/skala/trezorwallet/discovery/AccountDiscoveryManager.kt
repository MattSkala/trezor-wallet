package cz.skala.trezorwallet.discovery

import android.util.Log
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import java.security.InvalidKeyException


/**
 * Account discovery algorithm as defined in BIP 44.
 */
class AccountDiscoveryManager(val fetcher: TransactionFetcher) {
    companion object {
        const val GAP_SIZE = 20
        const val TAG = "AccountDiscoveryManager"
    }

    /**
     * Scans account external chain addresses for transactions.
     * @return True if any transactions are found, false otherwise.
     */
    fun scanAccount(node: TrezorType.HDNodeType): Boolean {
        try {
            val publicKey = node.publicKey.toByteArray()
            val chainCode = node.chainCode.toByteArray()

            val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(publicKey), chainCode)
            val externalChainNode = accountNode.deriveChildKey(0)

            return scanTransactionsForChainNode(externalChainNode)
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
        return false
    }

    private fun scanTransactionsForChainNode(externalChainNode: ExtendedPublicKey): Boolean {
        val addrs = mutableListOf<String>()
        for (addressIndex in 0 until GAP_SIZE) {
            val addressNode = externalChainNode.deriveChildKey(addressIndex)
            val address = addressNode.getAddress()
            addrs.add(address)
            Log.d(TAG, "/$addressIndex $address")
        }
        val page = fetcher.fetchTransactionsPage(addrs, 0, 50)
        return page.isNotEmpty()
    }
}