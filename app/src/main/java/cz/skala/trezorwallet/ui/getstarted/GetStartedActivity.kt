package cz.skala.trezorwallet.ui.getstarted

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.AppCompatActivityInjector
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.intents.ui.data.GenericResult
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.discovery.AccountDiscoveryManager
import cz.skala.trezorwallet.ui.MainActivity
import kotlinx.android.synthetic.main.activity_get_started.*


/**
 * An introductory activity where account discovery is performed after TREZOR device is connected.
 */
class GetStartedActivity : AppCompatActivity(), AppCompatActivityInjector {
    companion object {
        private const val REQUEST_INITIALIZE = 1
        private const val REQUEST_GET_PUBLIC_KEY = 2
    }

    override val injector = KodeinInjector()

    private val viewModel: GetStartedViewModel by injector.instance()

    override fun provideOverridingModule() = Kodein.Module {
        bind<GetStartedViewModel>() with provider {
            val factory = GetStartedViewModel.Factory(instance(), instance(), instance(), instance())
            ViewModelProviders.of(this@GetStartedActivity, factory)[GetStartedViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeInjector()

        setContentView(R.layout.activity_get_started)

        btnContinue.setOnClickListener {
            initializeConnection()
        }

        viewModel.onPublicKeyRequest.observe(this, Observer {
            if (it != null) {
                getPublicKeyForAccount(it.i, it.legacy)
            }
        })

        viewModel.onAccountDiscoveryFinish.observe(this, Observer {
            finishAccountDiscovery()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_INITIALIZE -> if (resultCode == Activity.RESULT_OK) {
                viewModel.startAccountDiscovery()
            }
            REQUEST_GET_PUBLIC_KEY -> if (resultCode == Activity.RESULT_OK) {
                val result = TrezorActivity.getResult(data) as GenericResult
                viewModel.onPublicKeyResult(result)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        destroyInjector()
        super.onDestroy()
    }

    private fun initializeConnection() {
        val intent = TrezorActivity.createGenericIntent(this,
                TrezorMessage.Initialize.getDefaultInstance())
        startActivityForResult(intent, REQUEST_INITIALIZE)
    }

    private fun getPublicKeyForAccount(i: Int, legacy: Boolean) {
        val request = AccountDiscoveryManager.createGetPublicKeyRequest(i, legacy)
        val intent = TrezorActivity.createIntent(this, request)
        startActivityForResult(intent, REQUEST_GET_PUBLIC_KEY)
    }

    private fun finishAccountDiscovery() {
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}