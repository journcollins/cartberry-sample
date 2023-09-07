package com.journcorp.journcart.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.databinding.LayoutCategoryHeaderBinding
import com.journcorp.journcart.databinding.ItemCategoryIconBinding
import com.journcorp.journcart.home.models.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CatIconAdapter(
    private val catClickListener: CatClickListener
) :
    ListAdapter<CatDataItem, RecyclerView.ViewHolder>(CatDiffCallback()) {
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
            is CatDataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is CatDataItem.CatItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {

            }
            is ViewHolder -> {
                val catItem = getItem(position) as CatDataItem.CatItem
                holder.bind(catItem.item, catClickListener)
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<Category>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(CatDataItem.Header)
                else -> listOf(CatDataItem.Header) + list.map { CatDataItem.CatItem(it) }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }

    }

    class ViewHolder private constructor(val binding: ItemCategoryIconBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Category, catClickListener: CatClickListener) {
            binding.item = item
            binding.clickListener = catClickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemCategoryIconBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }

    class HeaderViewHolder private constructor(val binding: LayoutCategoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = LayoutCategoryHeaderBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }
}

class CatDiffCallback : DiffUtil.ItemCallback<CatDataItem>() {
    override fun areItemsTheSame(oldItem: CatDataItem, newItem: CatDataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CatDataItem, newItem: CatDataItem): Boolean {
        return oldItem == newItem
    }
}

class CatClickListener(val clickListener: (catLink: Int) -> Unit) {
    fun onClick(item: Category) = clickListener(item.code)
}

sealed class CatDataItem {
    data class CatItem(val item: Category) : CatDataItem() {
        override val id = item.code
    }

    object Header : CatDataItem() {
        override val id = 0
    }

    abstract val id: Int
}