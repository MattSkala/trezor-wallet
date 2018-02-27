package cz.skala.trezorwallet.ui.transactions

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.entity.Transaction
import cz.skala.trezorwallet.ui.formatBtcValue
import kotlinx.android.synthetic.main.item_transaction.view.*

/**
 * Transactions list adapter.
 */
class TransactionsAdapter : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {
    var transactions: List<Transaction> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(transaction: Transaction) = with(itemView) {
            txtDateTime.text = transaction.getBlockTimeFormatted(resources)
            txtLabel.text = transaction.txid
            val sign = if (transaction.type == Transaction.Type.RECV) "+" else "−"
            val value = if (transaction.type == Transaction.Type.RECV) transaction.value
                else transaction.value + transaction.fee
            var valueFormatted = formatBtcValue(value)
            txtValueBtc.text = sign + resources.getString(R.string.x_btc, valueFormatted)
            val colorRes = when (transaction.type) {
                Transaction.Type.RECV -> R.color.colorPrimary
                else -> R.color.colorRed
            }
            txtValueBtc.setTextColor(resources.getColor(colorRes))
            txtValueUsd.visibility = View.GONE
        }
    }
}
