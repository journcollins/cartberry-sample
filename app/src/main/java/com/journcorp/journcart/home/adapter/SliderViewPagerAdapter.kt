package com.journcorp.journcart.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.databinding.ItemHomeSliderBinding
import com.journcorp.journcart.databinding.LayoutHomeTopAdBinding
import com.journcorp.journcart.home.models.HomeTopCarousel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SliderViewPagerAdapter(
    private val carouselClickListener: CarouselClickListener
) :
    ListAdapter<CarouselDataItem, RecyclerView.ViewHolder>(CarouselDiffCallback()) {
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
            is CarouselDataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is CarouselDataItem.CarouselItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {

            }
            is ViewHolder -> {
                val carouselItem = getItem(position) as CarouselDataItem.CarouselItem
                holder.bind(carouselItem.item, carouselClickListener)
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<HomeTopCarousel>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(CarouselDataItem.Header)
                else -> list.map {
                    CarouselDataItem.CarouselItem(
                        it
                    )
                }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }

    }

    class ViewHolder private constructor(val binding: ItemHomeSliderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeTopCarousel, carouselClickListener: CarouselClickListener) {
            binding.item = item
            binding.clickListener = carouselClickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemHomeSliderBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }

    class HeaderViewHolder private constructor(val binding: LayoutHomeTopAdBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = LayoutHomeTopAdBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }
}

class CarouselDiffCallback : DiffUtil.ItemCallback<CarouselDataItem>() {
    override fun areItemsTheSame(oldItem: CarouselDataItem, newItem: CarouselDataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CarouselDataItem, newItem: CarouselDataItem): Boolean {
        return oldItem == newItem
    }
}

class CarouselClickListener(val clickListener: (carouselLink: String) -> Unit) {
    fun onClick(item: HomeTopCarousel) = clickListener(item.carousel_link)
}

sealed class CarouselDataItem {
    data class CarouselItem(val item: HomeTopCarousel) : CarouselDataItem() {
        override val id = item.carousel_link
    }

    object Header : CarouselDataItem() {
        override val id = "0"
    }

    abstract val id: String
}