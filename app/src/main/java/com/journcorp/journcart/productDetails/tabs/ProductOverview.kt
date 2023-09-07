package com.journcorp.journcart.productDetails.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.databinding.FragmentProductOverviewBinding
import com.journcorp.journcart.productDetails.adapters.ProductOverviewAdapter
import com.journcorp.journcart.productDetails.dataClass.Product

class ProductOverview : Fragment() {
    private lateinit var binding: FragmentProductOverviewBinding
    private lateinit var productDetails: Product
    private lateinit var productId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductOverviewBinding.inflate(layoutInflater, container, false)
        productDetails = arguments?.getParcelable("mProductDetails")!!
        productId = arguments?.getString("productId").toString()

        val imageList = mutableListOf<String>()

        val mainImg = Gson().fromJson(productDetails.main_img, ArrayList::class.java)
        val imgUrl = Constants.storage_url + "/" + mainImg[0]
        imageList.add(imgUrl)

        val otherImages = Gson().fromJson(productDetails.other_img, ArrayList::class.java)
        otherImages.forEach {
            val otherImg = Gson().toJson(it)
            val finalOtherImage = Gson().fromJson(otherImg, ArrayList::class.java)
            val imgUrl2 = Constants.storage_url + "/" + finalOtherImage[0]
            imageList.add(imgUrl2)
        }

        val rvOverview = binding.rvOverview

        val adp = ProductOverviewAdapter(requireContext(), imageList)
        rvOverview.apply {
            adapter = adp
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL, false
            )
        }

        binding.loader.visibility = View.GONE

        return binding.root
    }
}