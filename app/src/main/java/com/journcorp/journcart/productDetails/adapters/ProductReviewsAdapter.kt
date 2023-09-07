package com.journcorp.journcart.productDetails.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.core.utils.GlideLoader
import com.journcorp.journcart.databinding.ItemReviewCommentBinding
import com.journcorp.journcart.productDetails.dataClass.ReviewDetails

class ProductReviewsAdapter(private val ReviewList: List<ReviewDetails>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //we return the inflated xml file that we have passed into the ViewHolder class constructor
        return ViewHolder(
            ItemReviewCommentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(ReviewList[position]) //we call the bind function inside the inner class "ViewHolder" and pass the details
    }

    override fun getItemCount(): Int = ReviewList.size

    inner class ViewHolder(itemView: ItemReviewCommentBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        private val ivFlag = itemView.ivFlag
        private val tvUserName = itemView.tvUserName
        private val rbRating = itemView.rbRating
        private val tvComment = itemView.tvComment
        private val ivCommentImg = itemView.ivCommentImg

        fun bind(review: ReviewDetails) {
            val flagName = review.country.lowercase()
            val flagImg =
                itemView.resources.getIdentifier(flagName, "drawable", itemView.context.packageName)

            GlideLoader(itemView.context).loadUserPicture(flagImg, ivFlag)
            val revName = review.fname[0]
            val revNameLen = review.fname.length
            val reductedName = revName + "*".repeat(revNameLen)
            tvUserName.text = reductedName
            rbRating.rating = review.stars.toFloat()
            tvComment.text = review.txt

            if (review.img.trim() != "") {
                ivCommentImg.visibility = View.VISIBLE
                val img = Gson().fromJson(review.img, List::class.java)
                val imgUrl = Constants.storage_url + "/" + img[1]
                GlideLoader(itemView.context).loadUserPicture(imgUrl, ivCommentImg)
            }
        }
    }
}
