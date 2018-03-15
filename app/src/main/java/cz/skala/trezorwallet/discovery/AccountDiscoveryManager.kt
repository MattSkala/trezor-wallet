package cz.skala.trezorwallet.discovery

import android.util.Log
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import java.security.InvalidKeyException


/**
 * Account discovery algorithm as defined in BIP 44.
 */
class AccountDiscoveryManager(val fetcher: TransactionFetcher) {
    companion object {
        const val TAG = "AccountDiscoveryManager"
        const val GAP_SIZE = 20

        const val PURPOSE_BIP44 = 44
        const val PURPOSE_BIP49 = 49
        const val COIN_BITCOIN = 0

        fun createGetPublicKeyRequest(i: Int, legacy: Boolean): TrezorRequest {
            val purpose = if (legacy) ExtendedPublicKey.HARDENED_IDX + PURPOSE_BIP44 else
                ExtendedPublicKey.HARDENED_IDX + PURPOSE_BIP49
            val coinType = ExtendedPublicKey.HARDENED_IDX + COIN_BITCOIN
            val account = ExtendedPublicKey.HARDENED_IDX + i
            val message = TrezorMessage.GetPublicKey.newBuilder()
                    .addAddressN(purpose.toInt())
                    .addAddressN(coinType.toInt())
                    .addAddressN(account.toInt())
                    .build()
            return GetPublicKeyRequest(message)
        }
    }

    /**
     * Scans account external chain addresses for transactions.
     * @return True if any transactions are found, false otherwise.
     */
    fun scanAccount(node: TrezorType.HDNodeType, legacy: Boolean): Boolean {
        try {
            val publicKey = node.publicKey.toByteArray()
            val chainCode = node.chainCode.toByteArray()

            val accountNode = ExtendedPublicKey(ExtendedPublicKey.decodePublicKey(publicKey), chainCode)
            val externalChainNode = accountNode.deriveChildKey(0)

            return scanTransactionsForChainNode(externalChainNode, legacy)
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
        return false
    }

    private fun scanTransactionsForChainNode(externalChainNode: ExtendedPublicKey, legacy: Boolean): Boolean {
        val addrs = mutableListOf<String>()
        for (addressIndex in 0 until GAP_SIZE) {
            val addressNode = externalChainNode.deriveChildKey(addressIndex)
            val address = if (legacy) {
                addressNode.getAddress()
            } else {
                addressNode.getSegwitAddress()
            }
            addrs.add(address)
            Log.d(TAG, "/$addressIndex $address ($legacy)")
        }
        val page = fetcher.fetchTransactionsPage(addrs, 0, 50)
        return page.isNotEmpty()
    }
}