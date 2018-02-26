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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_account, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int {
        return accounts.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(account: Account) = with(itemView) {
            txtAccountLabel.text = account.label ?: "Account #${account.index + 1}"
        }
    }
}