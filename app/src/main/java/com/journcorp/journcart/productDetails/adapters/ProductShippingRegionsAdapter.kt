package com.journcorp.journcart.productDetails.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.core.models.ShippingCosts
import com.journcorp.journcart.databinding.ItemRegionCostBinding
import com.journcorp.journcart.databinding.LayoutTextViewHeaderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductShippingRegionsAdapter(
    private val regionClickListener: RegionClickListener,
    private val mHeader: String = ""
) :
    ListAdapter<RegionDataItem, RecyclerView.ViewHolder>(RegionDiffCallback()) {
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_BODY = 1
    }

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder.from(parent)
            VIEW_TYPE_BODY -> ViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {

        return when (getItem(position)) {
            is RegionDataItem.Header -> VIEW_TYPE_HEADER
            is RegionDataItem.RegionItem -> VIEW_TYPE_BODY
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(mHeader)
            }
            is ViewHolder -> {
                val regionItem = getItem(position) as RegionDataItem.RegionItem
                holder.bind(
                    regionItem.item,
                    regionClickListener
                )
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<ShippingCosts>?) {
        list?.let {
            adapterScope.launch {
                if (list.isNotEmpty()) {
                    val items = listOf(RegionDataItem.Header) + list.map {
                        RegionDataItem.RegionItem(
                            it
                        )
                    }
                    withContext(Dispatchers.Main) {
                        submitList(items)
                    }
                }
            }
        }
    }

    class ViewHolder private constructor(val binding: ItemRegionCostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ShippingCosts,
            regionClickListener: RegionClickListener
        ) {
            binding.item = item
            binding.clickListener = regionClickListener

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemRegionCostBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }

    class HeaderViewHolder private constructor(val binding: LayoutTextViewHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: String) {
            binding.textViewHeader.text = header
        }

        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = LayoutTextViewHeaderBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }
}

class RegionDiffCallback : DiffUtil.ItemCallback<RegionDataItem>() {
    override fun areItemsTheSame(oldItem: RegionDataItem, newItem: RegionDataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RegionDataItem, newItem: RegionDataItem): Boolean {
        return oldItem == newItem
    }
}

class RegionClickListener(val clickListener: (regionLink: ShippingCosts) -> Unit) {
    fun onClick(item: ShippingCosts) = clickListener(item)
}

sealed class RegionDataItem {
    data class RegionItem(val item: ShippingCosts) : RegionDataItem() {
        override val id = item.region_id
    }

    object Header : RegionDataItem() {
        override val id = "0"
    }

    abstract val id: String
}
