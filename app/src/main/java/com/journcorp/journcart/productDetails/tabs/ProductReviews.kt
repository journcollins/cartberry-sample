package com.journcorp.journcart.productDetails.tabs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.databinding.FragmentProductReviewsBinding
import com.journcorp.journcart.productDetails.ProductViewModel
import com.journcorp.journcart.productDetails.adapters.ProductReviewsAdapter
import com.journcorp.journcart.productDetails.dataClass.Product
import com.journcorp.journcart.productDetails.dataClass.ReviewDetails

class ProductReviews(private val viewModel: ProductViewModel) : Fragment() {

    private lateinit var binding: FragmentProductReviewsBinding
    private lateinit var productDetails: Product
    private lateinit var productId: String

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductReviewsBinding.inflate(layoutInflater, container, false)

        productDetails = arguments?.getParcelable("mProductDetails")!!
        productId = arguments?.getString("productId").toString()

        val fiveStar = binding.progressBar1
        val fourStar = binding.progressBar2
        val threeStar = binding.progressBar3
        val twoStar = binding.progressBar4
        val oneStar = binding.progressBar5

        val fiveStarTxt = binding.tvPercent1
        val fourStarTxt = binding.tvPercent2
        val threeStarTxt = binding.tvPercent3
        val twoStarTxt = binding.tvPercent4
        val oneStarTxt = binding.tvPercent5

        var totalStars: Int =
            productDetails.one_star + productDetails.two_star + productDetails.three_star +
                    productDetails.four_star + productDetails.five_star
        totalStars = if (totalStars == 0) 1 else totalStars

        val fiveStarProgress = ((productDetails.five_star.toDouble() / totalStars) * 100)
        val fourStarProgress = ((productDetails.four_star.toDouble() / totalStars) * 100)
        val threeStarProgress = ((productDetails.three_star.toDouble() / totalStars) * 100)
        val twoStarProgress = ((productDetails.two_star.toDouble() / totalStars) * 100)
        val oneStarProgress = ((productDetails.one_star.toDouble() / totalStars) * 100)

        fiveStar.progress = fiveStarProgress.toInt()
        fourStar.progress = fourStarProgress.toInt()
        threeStar.progress = threeStarProgress.toInt()
        twoStar.progress = twoStarProgress.toInt()
        oneStar.progress = oneStarProgress.toInt()

        fiveStarTxt.text = "$fiveStarProgress%"
        fourStarTxt.text = "$fourStarProgress%"
        threeStarTxt.text = "$threeStarProgress%"
        twoStarTxt.text = "$twoStarProgress%"
        oneStarTxt.text = "$oneStarProgress%"

        viewModel.getReviewsList(Constants.deviceKey, productId, 0, 10)
        viewModel.reviewsListStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                    binding.loader.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.loader.visibility = View.GONE
                    val reviewData = it.data!!
                    val reviewList: MutableList<ReviewDetails> = mutableListOf()
                    reviewData.forEach { (img, stars, txt, country, fname) ->
                        reviewList.add(ReviewDetails(img, stars, txt, country, fname))
                    }

                    val rvReviews = binding.rvReviews
                    val adp = ProductReviewsAdapter(reviewList)
                    rvReviews.apply {
                        adapter = adp
                        layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.VERTICAL, false
                        )
                    }

                }
                else -> {
                    binding.loader.visibility = View.GONE
                }
            }
        }

        return binding.root
    }
}