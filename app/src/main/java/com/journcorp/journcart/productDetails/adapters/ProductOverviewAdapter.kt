package com.journcorp.journcart.productDetails.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.journcorp.journcart.R
import com.journcorp.journcart.databinding.ItemProductOverviewBinding

class ProductOverviewAdapter(private val context: Context, private val imageList: List<String>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //we return the inflated xml file that we have passed into the ViewHolder class constructor
        return ViewHolder(
            ItemProductOverviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(imageList[position]) //we call the bind function inside the inner class "ViewHolder" and pass the details
    }

    override fun getItemCount(): Int = imageList.size

    inner class ViewHolder(itemView: ItemProductOverviewBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        private val imageView = itemView.imageView

        fun bind(image: String) {
            Glide.with(context)
                .load(image)
                .fitCenter()
                .placeholder(R.drawable.leaves)
                .into(imageView)
        }
    }
}
