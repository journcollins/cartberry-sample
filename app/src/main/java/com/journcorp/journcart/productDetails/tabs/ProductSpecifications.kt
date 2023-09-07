package com.journcorp.journcart.productDetails.tabs

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.journcorp.journcart.databinding.FragmentProductSpecificationsBinding
import com.journcorp.journcart.productDetails.dataClass.Product

class ProductSpecifications : Fragment() {
    private lateinit var binding: FragmentProductSpecificationsBinding
    private lateinit var productDetails: Product
    private lateinit var productId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductSpecificationsBinding.inflate(layoutInflater, container, false)

        productDetails = arguments?.getParcelable("mProductDetails")!!
        productId = arguments?.getString("productId").toString()

        val tvPdtDesc = binding.tvProductDescription
        val tvPdtContainment = binding.tvProductContainment


        val pdtDescription = productDetails.txt_desc
        val spannedHtml: Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(pdtDescription, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(pdtDescription)
        }

        // Set the converted HTML text to your TextView
        tvPdtDesc.text = spannedHtml


        val pdtContainment = productDetails.containment
        tvPdtContainment.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(pdtContainment, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(pdtContainment)
        }
        binding.loader.visibility = View.GONE

        return binding.root
    }
}