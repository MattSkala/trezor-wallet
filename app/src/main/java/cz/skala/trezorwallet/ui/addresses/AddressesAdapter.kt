package cz.skala.trezorwallet.ui.addresses

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.skala.trezorwallet.R
import cz.skala.trezorwallet.data.entity.Address
import cz.skala.trezorwallet.data.item.AddressItem
import cz.skala.trezorwallet.data.item.ButtonItem
import cz.skala.trezorwallet.data.item.Item
import cz.skala.trezorwallet.data.item.SectionItem
import cz.skala.trezorwallet.ui.formatBtcValue
import kotlinx.android.synthetic.main.item_address.view.*
import kotlinx.android.synthetic.main.item_address_section.view.*
import kotlinx.android.synthetic.main.item_button.view.*

/**
 * Addresses list adapter.
 */
class AddressesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_SECTION = 1
        private const val TYPE_ADDRESS = 2
        private const val TYPE_BUTTON = 3
    }

    var items: List<Item> = mutableListOf()
    var onAddressClickListener: ((Address) -> Unit)? = null
    var onPreviousAddressesClickListener: (() -> Unit)? = null
    var onMoreAddressesClickListener: (() -> Unit)? = null

    fun updateItems(newItems: List<Item>) {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return newItems.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = items[oldItemPosition]
                val newItem = newItems[newItemPosition]
                return (oldItem is SectionItem && newItem is SectionItem && oldItem.title == newItem.title)
                        || oldItem == newItem
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newItems[newItemPosition]
            }
        })
        items = newItems
        result.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION -> {
                val view = inflater.inflate(R.layout.item_address_section, parent, false)
                SectionViewHolder(view)
            }
            TYPE_ADDRESS -> {
                val view = inflater.inflate(R.layout.item_address, parent, false)
                AddressViewHolder(view)
            }
            TYPE_BUTTON -> {
                val view = inflater.inflate(R.layout.item_button, parent, false)
                ButtonViewHolder(view)
            }
            else -> throw Exception("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is SectionItem -> {
                (holder as SectionViewHolder).bind(item.title, item.expandable, item.expanded)
                if (item.expandable) {
                    holder.itemView.setOnClickListener {
                        onPreviousAddressesClickListener?.invoke()
                    }
                    holder.itemView.isClickable = true
                } else {
                    holder.itemView.setOnClickListener(null)
                    holder.itemView.isClickable = false
                }
            }
            is AddressItem -> {
                (holder as AddressViewHolder).bind(item.address)
                holder.itemView.setOnClickListener {
                    onAddressClickListener?.invoke(item.address)
                }
            }
            is ButtonItem -> {
                (holder as ButtonViewHolder).bind(item.title, onMoreAddressesClickListener)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when(items[position]) {
            is SectionItem -> TYPE_SECTION
            is AddressItem -> TYPE_ADDRESS
            is ButtonItem -> TYPE_BUTTON
            else -> 0
        }
    }

    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(title: Int, expandable: Boolean, expanded: Boolean) = with(itemView) {
            txtTitle.setText(title)
            btnExpand.visibility = if (expandable) View.VISIBLE else View.GONE
            btnExpand.scaleY = if (expanded) -1f else 1f
        }
    }

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(address: Address) = with(itemView) {
            txtIndex.text = "/" + address.index.toString()
            txtLabel.text = address.address
            val receivedValue = formatBtcValue(address.totalReceived)
            val receivedText = SpannableStringBuilder(resources.getString(R.string.total_received, receivedValue))
            val receivedColor = resources.getColor(R.color.colorAccent)
            receivedText.setSpan(ForegroundColorSpan(receivedColor),
                    receivedText.length - receivedValue.length, receivedText.length, 0)
            txtTotalReceived.text = receivedText
        }
    }

    class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(title: Int, listener: (() -> Unit)?) = with(itemView) {
            btn.setText(title)
            btn.setOnClickListener {
                listener?.invoke()
            }
        }
    }
}
