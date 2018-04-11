package cz.skala.trezorwallet.ui.send

import android.app.Activity
import android.app.ProgressDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.SupportFragmentInjector
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.BitcoinURI
import cz.skala.trezorwallet.data.entity.FeeLevel
import cz.skala.trezorwallet.exception.InvalidBitcoinURIException
import cz.skala.trezorwallet.ui.MainActivity
import cz.skala.trezorwallet.ui.btcToSat
import kotlinx.android.synthetic.main.fragment_send.*
import java.util.*


/**
 * A fragment for composing a transaction.
 */
class SendFragment : Fragment(), SupportFragmentInjector {
    companion object {
        const val ARG_ACCOUNT_ID = "account_id"

        private const val TAG = "SendFragment"

        private const val REQUEST_SCAN_QR = 20
        private const val REQUEST_SIGN = 30

        private const val FEE_SPINNER_POSITION_CUSTOM = 4
    }

    override val injector = KodeinInjector()
    private val prefs: PreferenceHelper by injector.instance()
    private val viewModel: SendViewModel by injector.instance()

    private var textWatcherEnabled = true
    private var progressDialog: ProgressDialog? = null

    override fun provideOverridingModule() = Kodein.Module {
        bind<SendViewModel>() with provider {
            val factory = SendViewModel.Factory(instance(), instance(), instance(), instance(), instance(), instance())
            ViewModelProviders.of(this@SendFragment, factory)[SendViewModel::class.java]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeInjector()

        viewModel.start()

        viewModel.amountBtc.observe(this, Observer {
            if (it != null && !edtAmountBtc.isFocused) {
                if (it > 0) {
                    val value = java.lang.String.format(Locale.ENGLISH, "%.8f", it)
                    if (value != edtAmountBtc.text.toString()) {
                        textWatcherEnabled = false
                        edtAmountBtc.setText(value)
                        textWatcherEnabled = true
                    }
                } else {
                    edtAmountBtc.text = null
                }
            }
        })

        viewModel.amountUsd.observe(this, Observer {
            if (it != null && !edtAmountUsd.isFocused) {
                if (it > 0) {
                    val value = java.lang.String.format(Locale.ENGLISH, "%.2f", it)
                    if (value != edtAmountUsd.text.toString()) {
                        textWatcherEnabled = false
                        edtAmountUsd.setText(value)
                        textWatcherEnabled = true
                    }
                } else {
                    edtAmountUsd.text = null
                }
            }
        })

        viewModel.trezorRequest.observe(this, Observer {
            if (it != null) {
                val intent = TrezorActivity.createIntent(context!!, it)
                startActivityForResult(intent, REQUEST_SIGN)
            }
        })

        viewModel.sending.observe(this, Observer {
            if (it == true) {
                progressDialog = ProgressDialog.show(context, "Sending", "Please wait")
            } else {
                progressDialog?.dismiss()
            }
        })

        viewModel.onTxSent.observe(this, Observer {
            if (it != null) {
                Toast.makeText(context, "Transaction has been sent", Toast.LENGTH_LONG).show()
                edtAddress.text = null
                edtAmountBtc.text = null
                edtAmountUsd.text = null

                val accountId = arguments!!.getString(ARG_ACCOUNT_ID)
                (activity as MainActivity).showTransactions(accountId)
            } else {
                Toast.makeText(context!!, "Sending transaction failed", Toast.LENGTH_LONG).show()
            }
        })

        viewModel.onInsufficientFunds.observe(this, Observer {
            Toast.makeText(context, "Insufficient funds", Toast.LENGTH_LONG).show()
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnQrCode.setOnClickListener {
            startQrScanner()
        }

        btnSend.setOnClickListener {
            handleSendClick()
        }

        edtAmountBtc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (textWatcherEnabled) {
                    val value = s.toString().toDoubleOrNull() ?: 0.0
                    viewModel.setAmountBtc(value)
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        edtAmountUsd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (textWatcherEnabled) {
                    val value = s.toString().toDoubleOrNull() ?: 0.0
                    viewModel.setAmountUsd(value)
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        txtCurrencyCode.text = prefs.currencyCode

        initFeeSpinner()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SCAN_QR -> if (resultCode == Activity.RESULT_OK) {
                val scanResult = data!!.getStringExtra("SCAN_RESULT")
                Log.d(TAG, "scanResult: $scanResult")
                handleQrScanResult(scanResult)
            }
            REQUEST_SIGN -> if (resultCode == Activity.RESULT_OK) {
                val signedTx = data!!.getStringExtra(TrezorActivity.EXTRA_SIGNED_TX)
                Log.d(TAG, "signedTx: $signedTx")
                Log.d(TAG, "signedTx size: ${signedTx.length/2}")
                viewModel.sendTransaction(signedTx)
            } else {
                val failure = data?.getSerializableExtra(TrezorActivity.EXTRA_FAILURE) as TrezorMessage.Failure?
                if (failure != null) {
                    Toast.makeText(context, failure.message, Toast.LENGTH_LONG).show()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        destroyInjector()
        super.onDestroy()
    }

    private fun initFeeSpinner() {
        viewModel.recommendedFees.observe(this, Observer { fees ->
            if (fees != null) {
                val spinnerList = FeeLevel.values().map { level ->
                    val fee = fees[level]
                    resources.getString(level.titleRes) + " ($fee sat/B)"
                }.toMutableList()
                spinnerList.add(resources.getString(R.string.fee_custom))
                updateFeeSpinnerAdapter(spinnerList)
            }
        })

        spnFee.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isCustom = (position == FEE_SPINNER_POSITION_CUSTOM)
                edtFee.visibility = if (isCustom) View.VISIBLE else View.GONE
                txtFeeUnits.visibility = if (isCustom) View.VISIBLE else View.GONE
                if (isCustom) {
                    edtFee.requestFocus()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun updateFeeSpinnerAdapter(list: List<String>) {
        val spinnerAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, list)
        val selectedItem = if (spnFee.adapter != null) spnFee.selectedItemPosition else 1
        spnFee.adapter = spinnerAdapter
        spnFee.setSelection(selectedItem)
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

        if (spnFee.selectedItemPosition == FEE_SPINNER_POSITION_CUSTOM && edtFee.text.isEmpty()) {
            edtFee.error = "Missing fee"
            return
        }

        val account = arguments!!.getString(ARG_ACCOUNT_ID)
        val address = edtAddress.text.toString()
        val amount = edtAmountBtc.text.toString().toDouble()

        val fee = if (spnFee.selectedItemPosition == FEE_SPINNER_POSITION_CUSTOM) {
            edtFee.text.toString().toInt()
        } else {
            val selectedFeeLevel = FeeLevel.values()[spnFee.selectedItemPosition]
            viewModel.recommendedFees.value!![selectedFeeLevel]!!
        }

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

        viewModel.composeTransaction(account, address, btcToSat(amount), fee)
    }

    private fun startQrScanner() {
        val intent = Intent("com.google.zxing.client.android.SCAN")
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE")

        if (intent.resolveActivity(context!!.packageManager) != null) {
            startActivityForResult(intent, REQUEST_SCAN_QR)
        } else {
            val marketUri = Uri.parse("market://details?id=com.google.zxing.client.android")
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
            startActivity(marketIntent)
        }
    }

    private fun handleQrScanResult(scanResult: String) {
        try {
            val uri = BitcoinURI.parse(scanResult)
            val address = uri.address
            edtAddress.setText(address)

            if (uri.amount > 0) {
                viewModel.setAmountBtc(uri.amount)
            }
        } catch (e: InvalidBitcoinURIException) {
            e.printStackTrace()

            if (viewModel.validateAddress(scanResult)) {
                // Not a Bitcoin URI, but just a plain text address
                edtAddress.setText(scanResult)
            } else {
                Toast.makeText(context, "Invalid address", Toast.LENGTH_SHORT).show()
            }
        }
    }
}