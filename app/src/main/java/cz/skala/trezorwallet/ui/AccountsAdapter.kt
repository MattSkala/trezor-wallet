package cz.skala.trezorwallet.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.entity.Account
import kotlinx.android.synthetic.main.item_account.view.*

/**
 * Accounts list adapter.
 */
class AccountsAdapter : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {
    var accounts: List<Account> = mutableListOf()
    var selectedPosition = 0
    var onItemClickListener: ((account: Account) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_account, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(accounts[position], selectedPosition == position)
        holder.itemView.setOnClickListener {
            selectedPosition = holder.adapterPosition
            notifyDataSetChanged()
            onItemClickListener?.invoke(accounts[selectedPosition])
        }
    }

    override fun getItemCount(): Int {
        return accounts.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(account: Account, selected: Boolean) = with(itemView) {
            txtAccountLabel.text = account.label ?: "Legacy Account #${account.index + 1}"
            isSelected = selected
        }
    }
}