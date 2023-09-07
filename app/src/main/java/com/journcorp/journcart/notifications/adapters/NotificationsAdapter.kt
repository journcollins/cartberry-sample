package com.journcorp.journcart.notifications.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.databinding.ItemNotificationsBinding
import com.journcorp.journcart.databinding.LayoutTextViewHeaderBinding
import com.journcorp.journcart.notifications.models.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsAdapter(
    private val notificationsClickListener: NotificationsClickListener,
    private val mHeader: String = ""
) :
    ListAdapter<NotificationsDataItem, RecyclerView.ViewHolder>(NotificationsDiffCallback()) {
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
            is NotificationsDataItem.Header -> VIEW_TYPE_HEADER
            is NotificationsDataItem.NotificationsItem -> VIEW_TYPE_BODY
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(mHeader)
            }
            is ViewHolder -> {
                val notificationsItem = getItem(position) as NotificationsDataItem.NotificationsItem
                holder.bind(
                    notificationsItem.item,
                    notificationsClickListener
                )
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<Notifications>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(NotificationsDataItem.Header)
                else -> listOf(NotificationsDataItem.Header) + list.map {
                    NotificationsDataItem.NotificationsItem(
                        it
                    )
                }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }

    }

    class ViewHolder private constructor(val binding: ItemNotificationsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: Notifications,
            notificationsClickListener: NotificationsClickListener
        ) {
            binding.item = item
            binding.clickListener = notificationsClickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemNotificationsBinding.inflate(layoutInflater, parent, false)

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

class NotificationsDiffCallback : DiffUtil.ItemCallback<NotificationsDataItem>() {
    override fun areItemsTheSame(
        oldItem: NotificationsDataItem,
        newItem: NotificationsDataItem
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: NotificationsDataItem,
        newItem: NotificationsDataItem
    ): Boolean {
        return oldItem == newItem
    }
}

class NotificationsClickListener(val clickListener: (notificationsLink: Notifications) -> Unit) {
    fun onClick(item: Notifications) = clickListener(item)
}

sealed class NotificationsDataItem {
    data class NotificationsItem(val item: Notifications) : NotificationsDataItem() {
        override val id = item.url + item.date_added
    }

    object Header : NotificationsDataItem() {
        override val id = "0"
    }

    abstract val id: String
}