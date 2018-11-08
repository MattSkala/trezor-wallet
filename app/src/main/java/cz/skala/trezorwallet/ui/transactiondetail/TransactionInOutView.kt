package cz.skala.trezorwallet.ui.transactiondetail

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.ui.formatBtcValue
import kotlinx.android.synthetic.main.item_transaction_input.view.*

/**
 * A transaction input/output item.
 */
class TransactionInOutView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.item_transaction_input, this, true)
    }

    fun setValue(value: Long) {
        txtValue.text = formatBtcValue(value)
    }

    fun setAddress(address: String?) {
        txtAddress.text = address
    }

    fun setLabelEnabled(enabled: Boolean) {
        btnLabel.visibility = if (enabled) View.VISIBLE else View.GONE
    }
}