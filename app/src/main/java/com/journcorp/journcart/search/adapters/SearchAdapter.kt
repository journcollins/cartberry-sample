package com.journcorp.journcart.search.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.core.entities.Search
import com.journcorp.journcart.databinding.ItemSearchBinding
import com.journcorp.journcart.databinding.LayoutTextViewHeaderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchAdapter(
    private val searchClickListener: SearchClickListener,
    private val deleteClickListener: DeleteClickListener,
    private val mHeader: String = ""
) :
    ListAdapter<SearchDataItem, RecyclerView.ViewHolder>(SearchDiffCallback()) {
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
            is SearchDataItem.Header -> VIEW_TYPE_HEADER
            is SearchDataItem.SearchItem -> VIEW_TYPE_BODY
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(mHeader)
            }
            is ViewHolder -> {
                val searchItem = getItem(position) as SearchDataItem.SearchItem
                holder.bind(
                    searchItem.item,
                    searchClickListener,
                    deleteClickListener
                )
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<Search>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(SearchDataItem.Header)
                else -> listOf(SearchDataItem.Header) + list.map {
                    SearchDataItem.SearchItem(
                        it
                    )
                }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }

    }

    class ViewHolder private constructor(val binding: ItemSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: Search,
            searchClickListener: SearchClickListener,
            deleteClickListener: DeleteClickListener
        ) {
            binding.item = item
            binding.clickListener = searchClickListener
            binding.deleteClickListener = deleteClickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemSearchBinding.inflate(layoutInflater, parent, false)

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

class SearchDiffCallback : DiffUtil.ItemCallback<SearchDataItem>() {
    override fun areItemsTheSame(oldItem: SearchDataItem, newItem: SearchDataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SearchDataItem, newItem: SearchDataItem): Boolean {
        return oldItem == newItem
    }
}

class SearchClickListener(val clickListener: (searchLink: String) -> Unit) {
    fun onClick(item: Search) = item.title?.let { clickListener(it) }
}

class DeleteClickListener(val clickListener: (searchLink: Search) -> Unit) {
    fun onClick(item: Search) = clickListener(item)
}

sealed class SearchDataItem {
    data class SearchItem(val item: Search) : SearchDataItem() {
        override val id = item.ID
    }

    object Header : SearchDataItem() {
        override val id = "0"
    }

    abstract val id: String
}