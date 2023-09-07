package com.journcorp.journcart.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.databinding.ItemNavBarStoreBinding
import com.journcorp.journcart.databinding.LayoutTextViewHeaderSmallerBinding
import com.journcorp.journcart.home.models.NavBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NavBarAdapterAdapter(
    private val navbarClickListener: NavbarClickListener,
    private val mHeader: String = ""
) :
    ListAdapter<NavbarDataItem, RecyclerView.ViewHolder>(NavbarDiffCallback()) {
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
            is NavbarDataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is NavbarDataItem.NavbarItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(mHeader)
            }
            is ViewHolder -> {
                val navbarItem = getItem(position) as NavbarDataItem.NavbarItem
                holder.bind(navbarItem.item, navbarClickListener)
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<NavBar>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(NavbarDataItem.Header)
                else -> listOf(NavbarDataItem.Header) + list.map {
                    NavbarDataItem.NavbarItem(
                        it
                    )
                }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }

    }

    class ViewHolder private constructor(val binding: ItemNavBarStoreBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NavBar, navbarClickListener: NavbarClickListener) {
            binding.item = item
            binding.clickListener = navbarClickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemNavBarStoreBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }

    class HeaderViewHolder private constructor(val binding: LayoutTextViewHeaderSmallerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: String) {
            binding.textViewHeader.text = header
        }

        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = LayoutTextViewHeaderSmallerBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }
}

class NavbarDiffCallback : DiffUtil.ItemCallback<NavbarDataItem>() {
    override fun areItemsTheSame(oldItem: NavbarDataItem, newItem: NavbarDataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NavbarDataItem, newItem: NavbarDataItem): Boolean {
        return oldItem == newItem
    }
}

class NavbarClickListener(val clickListener: (navbarLink: String) -> Unit) {
    fun onClick(item: NavBar) = clickListener(item.url)
}

sealed class NavbarDataItem {
    data class NavbarItem(val item: NavBar) : NavbarDataItem() {
        override val id = item.url
    }

    object Header : NavbarDataItem() {
        override val id = "0"
    }

    abstract val id: String
}