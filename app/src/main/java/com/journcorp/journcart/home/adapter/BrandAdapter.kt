package com.journcorp.journcart.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.databinding.ItemBrandIconBinding
import com.journcorp.journcart.databinding.LayoutTextViewHeaderBinding
import com.journcorp.journcart.home.models.Brand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrandAdapter(
    private val brandClickListener: BrandClickListener,
    private val mHeader: String = ""
) :
    ListAdapter<BrandDataItem, RecyclerView.ViewHolder>(BrandDiffCallback()) {
    companion object {
        private const val ITEM_VIEW_TYPE_HEADER = 0
        private const val ITEM_VIEW_TYPE_ITEM = 1
    }

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> HeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> ViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is BrandDataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is BrandDataItem.BrandItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(mHeader)
            }
            is ViewHolder -> {
                val brandItem = getItem(position) as BrandDataItem.BrandItem
                holder.bind(brandItem.item, brandClickListener)
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<Brand>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(BrandDataItem.Header)
                else -> listOf(BrandDataItem.Header) + list.map {
                    BrandDataItem.BrandItem(
                        it
                    )
                }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }

    }

    class ViewHolder private constructor(val binding: ItemBrandIconBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Brand, brandClickListener: BrandClickListener) {
            binding.item = item
            binding.clickListener = brandClickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemBrandIconBinding.inflate(layoutInflater, parent, false)

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

class BrandDiffCallback : DiffUtil.ItemCallback<BrandDataItem>() {
    override fun areItemsTheSame(oldItem: BrandDataItem, newItem: BrandDataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BrandDataItem, newItem: BrandDataItem): Boolean {
        return oldItem == newItem
    }
}

class BrandClickListener(val clickListener: (brandLink: String) -> Unit) {
    fun onClick(item: Brand) = clickListener(item.id)
}

sealed class BrandDataItem {
    data class BrandItem(val item: Brand) : BrandDataItem() {
        override val id = item.id
    }

    object Header : BrandDataItem() {
        override val id = "0"
    }

    abstract val id: String
}