package com.journcorp.journcart.orders.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.R
import com.journcorp.journcart.databinding.ItemOrdersBinding
import com.journcorp.journcart.orders.models.Orders
import java.util.*

class OrdersAdapter(
    private val confirmClickListener: ConfirmClickListener,
    private val disputeClickListener: DisputeClickListener
) : PagingDataAdapter<Orders, OrdersAdapter.ViewHolder>(ProductComparator) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)!!
        holder.bind(item, confirmClickListener, disputeClickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder(val binding: ItemOrdersBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: Orders,
            confirmClickListener: ConfirmClickListener,
            disputeClickListener: DisputeClickListener
        ) {
            binding.item = item
            binding.confirmClickListener = confirmClickListener
            binding.disputeClickListener = disputeClickListener

            val tvOrderNumber = binding.tvOrderNumber
            tvOrderNumber.setOnClickListener {
                val clipboardManager = getSystemService(itemView.context,ClipboardManager::class.java) as ClipboardManager
                val clip = ClipData.newPlainText("order number", tvOrderNumber.text)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(itemView.context, itemView.resources.getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemOrdersBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }

    object ProductComparator : DiffUtil.ItemCallback<Orders>() {
        override fun areItemsTheSame(oldItem: Orders, newItem: Orders): Boolean {
            // Id is unique.
            return oldItem.order_id == newItem.order_id
        }

        override fun areContentsTheSame(oldItem: Orders, newItem: Orders): Boolean {
            return oldItem.order_status == newItem.order_status
        }
    }


}

class ConfirmClickListener(val clickListener: (id: String) -> Unit) {
    fun onClick(item: Orders) = clickListener(item.order_id)
}

class DisputeClickListener(val DisputeClickListener: (id: String) -> Unit) {
    fun onClick(item: Orders) = DisputeClickListener(item.order_id)
}

