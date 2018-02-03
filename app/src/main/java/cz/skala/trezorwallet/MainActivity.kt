package cz.skala.trezorwallet

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyRequest
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyResult
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import cz.skala.trezorwallet.crypto.ExtendedPublicKey
import timber.log.Timber
import java.security.InvalidKeyException

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        private const val REQUEST_GET_PUBLIC_KEY = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: move to application
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Log.d(TAG, "onCreate")

        setContentView(R.layout.activity_main)

        startAccountDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_GET_PUBLIC_KEY -> if (resultCode == RESULT_OK) {
                val result = TrezorActivity.getResult(data) as GetPublicKeyResult
                AlertDialog.Builder(this)
                        .setMessage(result.publicKey.xpub)
                        .show()

                startAccountDiscoveryForAccount(result.publicKey.node)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startAccountDiscovery() {
        val purpose = ExtendedPublicKey.HARDENED_IDX + 44 // BIP44
        val coinType = ExtendedPublicKey.HARDENED_IDX + 0 // Bitcoin
        val account = ExtendedPublicKey.HARDENED_IDX + 0
        val path = intArrayOf(purpose.toInt(), coinType.toInt(), account.toInt())
        val intent = TrezorActivity.createIntent(this@MainActivity,
                GetPublicKeyRequest(path))
        startActivityForResult(intent, REQUEST_GET_PUBLIC_KEY)
    }

    private fun startAccountDiscoveryForAccount(node: TrezorType.HDNodeType) {
        try {
            val accountNode = ExtendedPublicKey(
                    ExtendedPublicKey.decodePublicKey(node.publicKey.toByteArray()),
                    node.chainCode.toByteArray())
            val externalChainNode = accountNode.deriveChildKey(0)

            val addressIndex = 0
            val addressNode = externalChainNode.deriveChildKey(addressIndex)

            // TODO: serialize address
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
    }
}