package cz.skala.trezorwallet.ui.transactions

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.entity.Transaction
import cz.skala.trezorwallet.data.entity.TransactionWithInOut
import cz.skala.trezorwallet.data.item.AccountSummaryItem
import cz.skala.trezorwallet.data.item.DateItem
import cz.skala.trezorwallet.data.item.Item
import cz.skala.trezorwallet.data.item.TransactionItem
import cz.skala.trezorwallet.ui.formatBtcValue
import cz.skala.trezorwallet.ui.formatPrice
import kotlinx.android.synthetic.main.item_account_summary.view.*
import kotlinx.android.synthetic.main.item_transaction.view.*
import kotlinx.android.synthetic.main.item_transaction_date.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transactions list adapter.
 */
class TransactionsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_SUMMARY = 1
        private const val TYPE_DATE = 2
        private const val TYPE_TRANSACTION = 3
    }

    var items: List<Item> = mutableListOf()

    var onTransactionClickListener: ((TransactionWithInOut) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SUMMARY -> {
                val view = inflater.inflate(R.layout.item_account_summary, parent, false)
                SummaryViewHolder(view)
            }
            TYPE_DATE -> {
                val view = inflater.inflate(R.layout.item_transaction_date, parent, false)
                DateViewHolder(view)
            }
            TYPE_TRANSACTION -> {
                val view = inflater.inflate(R.layout.item_transaction, parent, false)
                TransactionViewHolder(view)
            }
            else -> throw Exception("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SummaryViewHolder -> {
                val item = items[position] as AccountSummaryItem
                holder.bind(item.summary, item.rate, item.currencyCode)
            }
            is DateViewHolder -> holder.bind((items[position] as DateItem).date)
            is TransactionViewHolder -> {
                val item = items[position] as TransactionItem
                holder.bind(item.transaction, item.rate, item.currencyCode)
                holder.itemView.setOnClickListener {
                    onTransactionClickListener?.invoke(item.transaction)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AccountSummaryItem -> TYPE_SUMMARY
            is DateItem -> TYPE_DATE
            is TransactionItem -> TYPE_TRANSACTION
            else -> 0
        }
    }

    class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(summary: AccountSummary, rate: Double, currencyCode: String) = with(itemView) {
            itemBalance.setTitle(R.string.balance)
            itemRate.setTitle(R.string.rate)
            itemReceived.setTitle(R.string.received)
            itemSent.setTitle(R.string.sent)

            val balance = summary.received - summary.sent
            itemBalance.setValuePrimary(formatBtcValue(balance))
            itemBalance.setValueSecondary(formatPrice(balance * rate, currencyCode))
            itemReceived.setValuePrimary(formatBtcValue(summary.received))
            itemReceived.setValueSecondary(formatPrice(summary.received * rate, currencyCode))
            itemSent.setValuePrimary(formatBtcValue(summary.sent))
            itemSent.setValueSecondary(formatPrice(summary.sent * rate, currencyCode))
            itemRate.setValuePrimary(formatPrice(rate, currencyCode))
            itemRate.setValueSecondary(formatBtcValue(1.0))
        }
    }

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(date: Date?) = with(itemView) {
            txtDate.text = if (date != null)
                SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG).format(date) else
                resources.getString(R.string.tx_unconfirmed)
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(transaction: TransactionWithInOut, rate: Double, currencyCode: String) = with(itemView) {
            txtDateTime.text = transaction.tx.getBlockTimeFormatted() ?:
                    resources.getString(R.string.tx_unconfirmed)

            val targets = transaction.vout.filter {
                when (transaction.tx.type) {
                    Transaction.Type.SENT -> !it.isMine
                    Transaction.Type.RECV -> it.isMine
                    Transaction.Type.SELF -> !it.isChange
                }
            }

            var label = ""
            targets.forEach {
                val addr = it.addr
                if (addr != null) {
                    if (label.isNotEmpty()) {
                        label += "\n"
                    }
                    label += addr
                }
            }

            txtLabel.text = label

            val sign = if (transaction.tx.type == Transaction.Type.RECV) "+" else "−"
            val value = if (transaction.tx.type == Transaction.Type.RECV) transaction.tx.value
                else transaction.tx.value + transaction.tx.fee
            txtValueBtc.text = sign + formatBtcValue(value)
            val colorRes = when (transaction.tx.type) {
                Transaction.Type.RECV -> R.color.colorPrimary
                else -> R.color.colorRed
            }
            txtValueBtc.setTextColor(resources.getColor(colorRes))
            txtValueUsd.text = formatPrice(value * rate, currencyCode)
            txtValueUsd.visibility = View.GONE
        }
    }
}
