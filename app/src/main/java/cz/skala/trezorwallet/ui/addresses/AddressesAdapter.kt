package cz.skala.trezorwallet.ui.addresses

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.entity.Address
import cz.skala.trezorwallet.ui.formatBtcValue
import kotlinx.android.synthetic.main.item_address.view.*

/**
 * Addresses list adapter.
 */
class AddressesAdapter : RecyclerView.Adapter<AddressesAdapter.ViewHolder>() {
    var addresses: List<Address> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_address, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount(): Int {
        return addresses.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(address: Address) = with(itemView) {
            txtIndex.text = "/" + address.index.toString()
            txtLabel.text = address.address
            val valueFormatted = resources.getString(R.string.x_btc, formatBtcValue(address.totalReceived))
            txtTotalReceived.text = resources.getString(R.string.total_received, valueFormatted)
        }
    }
}
