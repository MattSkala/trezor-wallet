package cz.skala.trezorwallet.discovery

import android.util.Log
import com.google.protobuf.ByteString
import com.satoshilabs.trezor.intents.ui.data.GenericRequest
import com.satoshilabs.trezor.intents.ui.data.TrezorRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.BuildConfig
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import cz.skala.trezorwallet.data.PreferenceHelper
import java.security.InvalidKeyException


/**
 * Account discovery algorithm as defined in BIP 44.
 */
class AccountDiscoveryManager(val fetcher: TransactionFetcher, val prefs: PreferenceHelper) {
    companion object {
        const val TAG = "AccountDiscoveryManager"
        const val GAP_SIZE = 20

        const val PURPOSE_BIP44 = 44
        const val PURPOSE_BIP49 = 49

        fun createGetPublicKeyRequest(i: Int, legacy: Boolean, deviceState: ByteString?): TrezorRequest {
            val purpose = if (legacy) ExtendedPublicKey.HARDENED_IDX + PURPOSE_BIP44 else
                ExtendedPublicKey.HARDENED_IDX + PURPOSE_BIP49
            val coinType = ExtendedPublicKey.HARDENED_IDX + BuildConfig.COIN_TYPE
            val account = ExtendedPublicKey.HARDENED_IDX + i
            val message = TrezorMessage.GetPublicKey.newBuilder()
                    .addAddressN(purpose.toInt())
                    .addAddressN(coinType.toInt())
                    .addAddressN(account.toInt())
                    .build()
            return GenericRequest(message, deviceState)
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
        val page = fetcher.fetchTransactions(addrs, 1)
        return page.isNotEmpty()
    }
}