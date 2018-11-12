package com.mattskala.trezorwallet.ui.transactions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.mattskala.trezorwallet.R
import kotlinx.android.synthetic.main.item_account_summary_item.view.*

class AccountSummaryItemView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.item_account_summary_item, this, true)
    }

    fun setTitle(title: Int) {
        txtTitle.setText(title)
    }

    fun setValuePrimary(value: String) {
        txtValuePrimary.text = value
    }

    fun setValueSecondary(value: String) {
        txtValueSecondary.text = value
    }
}