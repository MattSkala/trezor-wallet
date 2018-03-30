package cz.skala.trezorwallet.ui.send

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
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
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.SupportFragmentInjector
import com.satoshilabs.trezor.intents.ui.activity.TrezorActivity
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.PreferenceHelper
import cz.skala.trezorwallet.data.entity.FeeLevel
import kotlinx.android.synthetic.main.fragment_send.*
import java.util.*


/**
 * A fragment for composing a transaction.
 */
class SendFragment : Fragment(), SupportFragmentInjector {
    companion object {
        const val ARG_ACCOUNT_ID = "account_id"

        private const val REQUEST_SIGN = 30
        private const val FEE_SPINNER_POSITION_CUSTOM = 4
    }

    override val injector = KodeinInjector()
    private val prefs: PreferenceHelper by injector.instance()
    private val viewModel: SendViewModel by injector.instance()

    override fun provideOverridingModule() = Kodein.Module {
        bind<SendViewModel>() with provider {
            val factory = SendViewModel.Factory(instance(), instance(), instance())
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
                    edtAmountBtc.setText(value)
                } else {
                    edtAmountBtc.text = null
                }
            }
        })

        viewModel.amountUsd.observe(this, Observer {
            if (it != null && !edtAmountUsd.isFocused) {
                if (it > 0) {
                    val value = java.lang.String.format(Locale.ENGLISH, "%.2f", it)
                    edtAmountUsd.setText(value)
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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSend.setOnClickListener {
            handleSendClick()
        }

        edtAmountBtc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val value = s.toString().toDoubleOrNull() ?: 0.0
                viewModel.setAmountBtc(value)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        edtAmountUsd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val value = s.toString().toDoubleOrNull() ?: 0.0
                viewModel.setAmountUsd(value)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        txtCurrencyCode.text = prefs.currencyCode

        initFeeSpinner()
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

        viewModel.composeTransaction(account, address, amount, fee)
    }
}