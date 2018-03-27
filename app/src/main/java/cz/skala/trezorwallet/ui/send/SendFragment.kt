package cz.skala.trezorwallet.ui.send

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.SupportFragmentInjector
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import cz.skala.trezorwallet.R
import kotlinx.android.synthetic.main.fragment_send.*


/**
 * A fragment for composing a transaction.
 */
class SendFragment : Fragment(), SupportFragmentInjector {
    companion object {
        const val ARG_ACCOUNT_ID = "account_id"

        const val REQUEST_SIGN = 30
    }

    override val injector = KodeinInjector()
    private val viewModel: SendViewModel by injector.instance()

    override fun provideOverridingModule() = Kodein.Module {
        bind<SendViewModel>() with provider {
            val factory = SendViewModel.Factory(instance())
            ViewModelProviders.of(this@SendFragment, factory)[SendViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeInjector()

        viewModel.trezorRequest.observe(this, Observer {
            if (it != null) {
                val intent = TrezorActivity.createIntent(context!!, it)
                startActivityForResult(intent, REQUEST_SIGN)
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSend.setOnClickListener {
            handleSendClick()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SIGN -> if (resultCode == Activity.RESULT_OK) {
                val signedIx = data!!.getStringExtra(TrezorActivity.EXTRA_SIGNED_TX)
                Log.d("SendFragment", "signedTx: $signedIx")
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        destroyInjector()
        super.onDestroy()
    }

    private fun handleSendClick() {
        if (edtAddress.text.isEmpty()) {
            edtAddress.error = "Missing address"
            return
        }

        if (edtAmountBtc.text.isEmpty()) {
            edtAmountBtc.error = "Missing amount"
            return
        }

        if (edtFee.text.isEmpty()) {
            edtFee.error = "Missing fee"
            return
        }

        val account = arguments!!.getString(ARG_ACCOUNT_ID)
        val address = edtAddress.text.toString()
        val amount = edtAmountBtc.text.toString().toDouble()
        val fee = edtFee.text.toString().toInt()

        if (!viewModel.validateAddress(address)) {
            edtAddress.error = "Invalid address"
            return
        }

        if (!viewModel.validateAmount(amount)) {
            edtAmountBtc.error = "Invalid amount"
            return
        }

        if (!viewModel.validateFee(fee)) {
            edtFee.error = "Invalid fee"
            return
        }

        viewModel.composeTransaction(account, address, amount, fee)
    }
}