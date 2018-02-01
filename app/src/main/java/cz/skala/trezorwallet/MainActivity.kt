package cz.skala.trezorwallet

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyRequest
import com.satoshilabs.trezor.intents.ui.data.GetPublicKeyResult
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = "MainActivity"

        private val REQUEST_GET_PUBLIC_KEY = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: move to application
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Log.d(TAG, "onCreate")

        setContentView(R.layout.activity_main)

        val path = intArrayOf(44, 0, 0)
        val intent = TrezorActivity.createIntent(this@MainActivity,
                GetPublicKeyRequest(path))
        startActivityForResult(intent, REQUEST_GET_PUBLIC_KEY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_GET_PUBLIC_KEY -> if (resultCode == RESULT_OK) {
                val result = TrezorActivity.getResult(data) as GetPublicKeyResult
                AlertDialog.Builder(this)
                        .setMessage(result.xPubKey)
                        .show()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
