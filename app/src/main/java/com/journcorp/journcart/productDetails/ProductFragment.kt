package com.journcorp.journcart.productDetails

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.journcorp.journcart.R
import com.journcorp.journcart.brands.BrandsFragment
import com.journcorp.journcart.categories.CategoriesFragment
import com.journcorp.journcart.core.activities.WebViewActivity
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.fragments.BaseOtherFragment
import com.journcorp.journcart.core.utils.*
import com.journcorp.journcart.databinding.DialogSelectRegionBinding
import com.journcorp.journcart.databinding.FragmentProductBinding
import com.journcorp.journcart.events.EventsFragment
import com.journcorp.journcart.productDetails.adapters.ProductShippingRegionsAdapter
import com.journcorp.journcart.productDetails.adapters.ProductTabsPagerAdapter
import com.journcorp.journcart.productDetails.adapters.ProductViewPagerAdapter
import com.journcorp.journcart.productDetails.adapters.RegionClickListener
import com.journcorp.journcart.productDetails.dataClass.*
import com.journcorp.journcart.productDetails.tabs.ProductOverview
import com.journcorp.journcart.productDetails.tabs.ProductReviews
import com.journcorp.journcart.productDetails.tabs.ProductSpecifications
import com.journcorp.journcart.store.StoreFragment
import com.journcorp.journcart.watchlist.WatchlistFragment
import com.nex3z.flowlayout.FlowLayout
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.*
import kotlin.math.abs
import kotlin.math.floor


class ProductFragment(
    private val productId: String = ""
) : BaseOtherFragment(), View.OnClickListener {
    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var job: Job
    private lateinit var uiScope: CoroutineScope
    private lateinit var backgroundScope: CoroutineScope

    private lateinit var productShippingRegionsAdapter: ProductShippingRegionsAdapter
    private lateinit var mPlaceholderDialog: Dialog
    private lateinit var mProductDetails: Product
    private val shippingMethodIds: HashMap<Int, String> = hashMapOf()
    private var totalShippingRegions: Int = 0

    private lateinit var viewModel: ProductViewModel
    private var mSelectedVar0: Var0? = Var0()
    private var mSelectedVar1: Var1? = Var1()
    private var mSelectedVar2: Var2? = Var2()

    private val var0Data = hashMapOf<Int, Var0>()
    private val var1Data = hashMapOf<Int, Var1>()
    private val var2Data = hashMapOf<Int, Var2>()

    private var mShippingTotal: Double = 0.0
    private var mVar0SubTotal: Double = 0.0
    private var mVar1SubTotal: Double = 0.0
    private var mVar2SubTotal: Double = 0.0

    private lateinit var viewPagerAdapter: ProductViewPagerAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)

        job = Job()
        uiScope = CoroutineScope(Dispatchers.Main + job)
        backgroundScope = CoroutineScope(Dispatchers.IO + job)

        val dataSource = MainRoomDatabase.getInstance(requireActivity().application)
        val viewModelFactory = ProductViewModelFactory(
            dataSource = dataSource, requireActivity().application, productId
        )
        viewModel = ViewModelProvider(this, viewModelFactory)[ProductViewModel::class.java]
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        viewModel.getProductDetails(productId)

        binding.highestBidderNotification.setOnClickListener {
            addFragment(WatchlistFragment())
        }

        binding.visitStore.setOnClickListener {
            addFragment(StoreFragment(mProductDetails.url))
        }

        observers()

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observers() {
        viewModel.productDataStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    mProductDetails = it.data!!

                    viewModel.getCountShippingRegions(it.data.shipping_id)
                    setUpViews()
                }
                is Resource.Error -> {
                    showErrorSnackBar(it.message.toString(), true)
                }
            }
        }

        viewModel.shippingRegionsCountDataStatus.observe(viewLifecycleOwner){
            totalShippingRegions = it
        }

        viewModel.auctionDetails.observe(viewLifecycleOwner) {
            binding.tvCurrentBid.text = it["unitPrice"]

            if (it["isMe"] == "1.0" || it["isMe"] == "1") {
                binding.highestBidderNotification.visibility = View.VISIBLE
            } else {
                binding.highestBidderNotification.visibility = View.GONE
            }

        }

        viewModel.countryDataStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                    Toast.makeText(
                        activity, resources.getString(R.string.please_wait), Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Success -> {
                    val shippingData = it.data!!
                    mProductDetails.shipping = shippingData.shipping
                    mProductDetails.shipping_id = shippingData.shipping_id
                    mProductDetails.shipping_type = shippingData.shipping_type
                    mProductDetails.shipping_name = shippingData.shipping_name
                    mProductDetails.shipping_regions = shippingData.shipping_regions
                    mProductDetails.estimated_time_in = shippingData.estimated_time_in
                    mProductDetails.estimated_time_out = shippingData.estimated_time_out

                    Toast.makeText(
                        activity, resources.getString(R.string.success), Toast.LENGTH_SHORT
                    ).show()
                    shippingArea(
                        mProductDetails.shipping,
                        mProductDetails.shipping_id,
                        mProductDetails.shipping_type,
                        mProductDetails.shipping_name,
                        mProductDetails.shipping_regions
                    )
                }
                is Resource.Error -> {
                    showErrorSnackBar(it.message.toString(), true)
                }
            }
        }

        viewModel.getShippingDataStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                    Toast.makeText(
                        activity, resources.getString(R.string.please_wait), Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Success -> {
                    val shippingData = it.data!!

                    showShippingMethodsDialog(shippingData)
                }
                is Resource.Error -> {
                    showErrorSnackBar(it.message.toString(), true)
                }
            }
        }

        viewModel.setShippingDataStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                    Toast.makeText(
                        activity, resources.getString(R.string.please_wait), Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Success -> {
                    val shippingData = it.data!!
                    mProductDetails.shipping = shippingData.shipping
                    mProductDetails.shipping_id = shippingData.shipping_id
                    mProductDetails.shipping_type = shippingData.shipping_type
                    mProductDetails.shipping_name = shippingData.shipping_name
                    mProductDetails.shipping_regions = shippingData.shipping_regions
                    mProductDetails.estimated_time_in = shippingData.estimated_time_in
                    mProductDetails.estimated_time_out = shippingData.estimated_time_out

                    Toast.makeText(
                        activity, resources.getString(R.string.success), Toast.LENGTH_SHORT
                    ).show()
                    shippingArea(
                        mProductDetails.shipping,
                        mProductDetails.shipping_id,
                        mProductDetails.shipping_type,
                        mProductDetails.shipping_name,
                        mProductDetails.shipping_regions
                    )
                    updateTotalPrice()
                }
                is Resource.Error -> {
                    showErrorSnackBar(it.message.toString(), true)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "ResourceType")
    private fun setUpViews() {
        val mImageList: MutableList<ProductTopCarouselModel> = mutableListOf()
        val vid = Gson().fromJson(mProductDetails.vid, ArrayList::class.java)
        vid?.let {
            mImageList.add(ProductTopCarouselModel(vid[1], vid[0]))
        }

        val mainImg = Gson().fromJson(mProductDetails.main_img, ArrayList::class.java)
        val otherImages = Gson().fromJson(mProductDetails.other_img, ArrayList::class.java)

        mImageList.add(ProductTopCarouselModel(null, mainImg[1]))

        otherImages.forEach {
            val otherImg = Gson().toJson(it)
            val finalOtherImage = Gson().fromJson(otherImg, ArrayList::class.java)
            mImageList.add(ProductTopCarouselModel(null, finalOtherImage[1]))
        }

        viewPagerAdapter = ProductViewPagerAdapter(requireActivity(), mImageList)
        binding.vpProduct.apply {
            adapter = viewPagerAdapter
        }

        //TLDots
        TabLayoutMediator(binding.TLDots, binding.vpProduct) { tab, position ->
            // Set the tab text to the position of the page
            tab.text = (position + 1).toString()
        }.attach()

        TabLayoutMediator(binding.TLDots, binding.vpProduct) { tab, _ ->
            tab.setIcon(R.drawable.baseline_viewpager_indicator)
        }.attach()

        binding.tvPdtName.text = mProductDetails.pdt_name
        binding.tvPdtCondition.text = when (mProductDetails.pdt_condition) {
            1 -> resources.getString(R.string.new_product)
            2 -> resources.getString(R.string.old_product)
            3 -> resources.getString(R.string.refurbished_product)
            else -> "N/A"
        }

        binding.tvPdtCat.apply {
            text = CCFunctions.getCatString(mProductDetails.category, requireContext())
            setOnClickListener {
                addFragment(CategoriesFragment(mProductDetails.category))
            }
        }

        binding.tvPdtBrand.text = when (mProductDetails.brand_name) {
            "" -> "0"
            else -> {
                binding.tvPdtBrand.setOnClickListener {
                    val brandId = mProductDetails.brand_id
                    addFragment(BrandsFragment(brandId))
                }
                mProductDetails.brand_name
            }
        }

        if (mProductDetails.event_tag == 0) {
            binding.tvPdtTag.visibility = View.GONE
        } else {
            binding.tvPdtTag.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    addFragment(EventsFragment(mProductDetails.url))
                }
            }
        }

        //DISCOUNT AREA STARTS HERE
        binding.tvPdtPrice.text =
            CCFunctions.formatCurrency(
                mProductDetails.unit_price,
                mProductDetails.currency
            )

        if (mProductDetails.pdt_type > 2) {
            val content1 =
                CCFunctions.formatCurrency(
                    mProductDetails.old_unit_price,
                    mProductDetails.currency
                )
            val spannableString1 = SpannableString(content1)
            spannableString1.setSpan(StrikethroughSpan(), 0, content1.length, 0)
            binding.tvPdtOldPrice.apply {
                text = spannableString1
                visibility = View.VISIBLE
            }

            val percentDiscount =
                (abs(mProductDetails.old_unit_price - mProductDetails.unit_price) / mProductDetails.old_unit_price) * 100

            val roundedPercent = percentDiscount.toInt()

            binding.tvDiscountPercent.apply {
                text = if (roundedPercent < 0) {
                    val percentLabel = resources.getString(R.string.percentage_added)
                    "${roundedPercent}% $percentLabel"
                } else {
                    val percentLabel = resources.getString(R.string.percentage_off)
                    "${roundedPercent}% $percentLabel"
                }
                visibility = View.VISIBLE
            }
        } else {
            binding.tvDiscountPercent.visibility = View.GONE
        }
        //DISCOUNT AREA ENDS HERE

        var totalStars =
            mProductDetails.one_star + mProductDetails.two_star + mProductDetails.three_star + mProductDetails.four_star + mProductDetails.five_star

        val totalReviews = totalStars
        totalStars =
            if (totalStars > 0) totalStars else 1 //make it a one coz dividing by 0 is mathematically wrong
        val weightedTotalStars: Int =
            ((mProductDetails.one_star * 1) + (mProductDetails.two_star * 2) + (mProductDetails.three_star * 3) + (mProductDetails.four_star * 4) + (mProductDetails.five_star * 5))
        val stars: Float = weightedTotalStars.toFloat() / totalStars
        binding.rbRating.rating = stars

        binding.tvReviewScore.text = String.format("%.1f", stars)

        val reviewsLabel = resources.getString(R.string.reviews)
        binding.tvReviewCount.text = "$totalReviews $reviewsLabel"

        val ordersLabel = resources.getString(R.string.pdt_orders)
        binding.tvOrderCount.text = "${mProductDetails.orders} $ordersLabel"

        //TODO WORK ON SHIPPING PART HERE
        shippingArea(
            mProductDetails.shipping,
            mProductDetails.shipping_id,
            mProductDetails.shipping_type,
            mProductDetails.shipping_name,
            mProductDetails.shipping_regions
        )

        //TODO WORK ON VARIATIONS HERE

        if (mProductDetails.pdt_type != 2) {// not auction
            normalOrDealProduct()
        } else {//AUCTION PRODUCT
            auctionProduct()
        }


        binding.fabWishlist.setOnClickListener {
            addToWishlist()
        }

        binding.tvChangeShippingMethod.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                viewModel.getShippingMethods(
                    Constants.deviceKey, productId, mProductDetails.shipping
                )
            }
        }

        binding.ccpChangeCountry.apply {
            visibility = View.VISIBLE

            var country = "UG"//DEFAULT COUNTRY
            if (Constants.userCountry.length == 3) {
                country = CCFunctions.getMapKey(Constants.iso2toiso3, Constants.userCountry)
            }
            setCountryForNameCode(country)

            setOnCountryChangeListener {
                val countryCode = this.selectedCountryNameCode as String
                val finalCountry = Constants.iso2toiso3[countryCode]!!

                viewModel.changeUserCountry(Constants.deviceKey, productId, finalCountry)
                Constants.userCountry = finalCountry

                shippingArea(
                    mProductDetails.shipping,
                    mProductDetails.shipping_id,
                    mProductDetails.shipping_type,
                    mProductDetails.shipping_name,
                    mProductDetails.shipping_regions
                )
                updateTotalPrice()

                uiScope.launch {
                    Constants.mUserData.get()?.let {
                        val userData = it.copy(
                            country = finalCountry
                        )

                        BasicLocalStorage(requireActivity()).setUserData(userData)
                    }
                }
            }
        }

        binding.tvChangeRegion.apply {
            text = resources.getString(R.string.change_region)
            setOnClickListener {
                showShippingRegionsDialog()
            }
        }

        productTabs()
    }

    private fun shippingArea(
        shipping: String,
        shipping_id: String,
        shipping_type: Int,
        shipping_name: String,
        shipping_regions: String
    ) {
        mShippingTotal = 0.0//resets the value whenever function is called
        val shippingList = Gson().fromJson(shipping, List::class.java)
        var found = false

        for (n in shippingList) {
            if (n == shipping_id) {
                found = true
                break
            }
        }

        if (!found) {//shipping id isnt valid in shipping methods
            return
        }

        val shippingRegions = Gson().fromJson(shipping_regions, HashMap::class.java)
        var shippingMessage = ""
        var shippingDetails = ""

        if (shippingRegions == null) {
            shippingMessage = resources.getString(
                R.string.cannot_ship_to_via,
                Constants.countries[Constants.userCountry],
                shipping_name
            )
        }

        shippingRegions?.let {
            if (shippingRegions[Constants.userCountry] == null) {//cannot ship to given country
                shippingMessage = resources.getString(
                    R.string.cannot_ship_to_via,
                    Constants.countries[Constants.userCountry],
                    shipping_name
                )
            } else {
                if (shipping_type == 1) {//regional shipping
                    binding.tvChangeRegion.visibility = View.VISIBLE
                    val userRegion = Constants.userRegion

                    val countryToJson = Gson().toJson(shippingRegions[Constants.userCountry])

                    val packagesArray =
                        Gson().fromJson(countryToJson, HashMap::class.java)

                    var storedRegionId = ""
                    var storedRegionName = ""
                    var storedRegionCost = -1.0

                    for (packageArray in packagesArray) {
                        val v1 = Gson().toJson(packageArray)
                        val v2 = Gson().fromJson(v1, HashMap::class.java)["value"]

                        val v3 = Gson().toJson(v2)
                        val regionsUnJson = Gson().fromJson(v3, HashMap::class.java)

                        try {
                            storedRegionId = regionsUnJson["id"].toString()
                            storedRegionName = regionsUnJson["city"].toString()
                            storedRegionCost = regionsUnJson["storeCost"].toString().toDouble()
                        } catch (_: java.lang.NullPointerException) {

                        }

                        if (userRegion == storedRegionId || userRegion.isEmpty()) {//if ids match or region not set
                            break
                        }
                    }
                    Constants.userRegion = storedRegionId

                    //productFinalShipping += storedRegionCost// increment the region cost if regional
                    if (storedRegionCost < 0) {//not found any regions inside regional shipping guy
                        shippingMessage = resources.getString(
                            R.string.cannot_ship_to_via,
                            Constants.countries[Constants.userCountry],
                            shipping_name
                        )
                    }
                    mShippingTotal = storedRegionCost

                    if (storedRegionCost == 0.0) {//free shipping
                        shippingDetails = resources.getString(
                            R.string.free_shipping_details_regional,
                            Constants.countries[Constants.userCountry],
                            storedRegionName,
                            shipping_name
                        )
                    } else {
                        val shippingCostFormatted =
                            CCFunctions.formatCurrency(
                                storedRegionCost,
                                mProductDetails.shipping_currency
                            )

                        shippingDetails = resources.getString(
                            R.string.shipping_details_regional,
                            shippingCostFormatted,
                            Constants.countries[Constants.userCountry],
                            storedRegionName,
                            shipping_name
                        )
                    }

                } else {//International
                    binding.tvChangeRegion.visibility = View.GONE

                    val v1 = Gson().toJson(shippingRegions[Constants.userCountry])
                    val v2 = Gson().fromJson(v1, HashMap::class.java)

                    val shippingPrice = v2["storeCost"].toString().toDouble()
                    //productFinalShipping += shippingPrice //increment the international shipping cost if international

                    mShippingTotal = shippingPrice

                    if (shippingPrice == 0.0) {//free shipping
                        shippingDetails = resources.getString(
                            R.string.free_shipping_details_international,
                            Constants.countries[Constants.userCountry],
                            shipping_name
                        )
                    } else {
                        val shippingCostFormatted =
                            CCFunctions.formatCurrency(
                                shippingPrice,
                                mProductDetails.currency
                            )

                        shippingDetails = resources.getString(
                            R.string.shipping_details_international,
                            shippingCostFormatted,
                            Constants.countries[Constants.userCountry],
                            shipping_name
                        )
                    }

                }
            }
        }

        if (shippingMessage == "") {//can ship to that country
            binding.tvShippingDetails.apply {
                text = shippingDetails
                visibility = View.VISIBLE
            }
            binding.tvShippingMessage.visibility = View.GONE
        } else {//cannot ship there so display the message
            binding.tvShippingMessage.apply {
                text = shippingMessage
                visibility = View.VISIBLE
            }
            binding.tvShippingDetails.visibility = View.GONE
        }

    }

    @SuppressLint("SetTextI18n")
    private fun normalOrDealProduct() {
        binding.tvCurrentBidLabel.visibility = View.GONE
        binding.tilBidInput.visibility = View.GONE
        binding.tvCurrentBid.visibility = View.GONE
        binding.btnSubmitBid.visibility = View.GONE
        binding.tvAuctionTime.visibility = View.GONE

        binding.totalLabel.visibility = View.VISIBLE
        binding.pdtTotal.visibility = View.VISIBLE
        binding.addToCart.visibility = View.VISIBLE

        val json = mProductDetails.vars_arr
        val finalOne = Gson().fromJson(json, ProductVars::class.java)
        //val jsonObject = JSONObject(mProductDetails.vars_arr)
        val highestArray = doubleArrayOf(0.0, 0.0, 0.0)

        var varsHeader0 = ""
        var variableId = 0//variable id
        var variableExists = false
        finalOne.var0.forEach { (variableType, variableInitial, varInitialStatus, variableName, variableQty, variablePrice, storeVariablePrice) ->
            if (variableQty >= 0 && variableName.trim() != "") {
                val finalVariablePrice = if (Constants.userCurrency == mProductDetails.currency)
                    storeVariablePrice
                else
                    variablePrice

                mSelectedVar0 = null//is set to null if there is a variable to be selected
                //val status = if (varInitialStatus == "txt") 1 else 2
                highestArray[0] =
                    if (highestArray[0] > finalVariablePrice) highestArray[0] else finalVariablePrice
                varsHeader0 = variableType
                addVariable(
                    "var0", binding.llVariationBody0, variableId, varInitialStatus, variableInitial
                )
                var0Data[variableId] = Var0(
                    variableType,
                    variableInitial,
                    varInitialStatus,
                    variableName,
                    variableQty,
                    finalVariablePrice
                )
                variableExists = true
            }
            ++variableId
        }
        if (variableId > 0) {//show variable 0 space
            binding.tvVariationTitle0.text = varsHeader0

            if (variableExists) {
                binding.tvVariationTitle0.visibility = View.VISIBLE
                binding.tvSelectedVariation0.visibility = View.VISIBLE
                binding.llVariationBody0.visibility = View.VISIBLE
            }
        }

        var varsHeader1 = ""
        variableId = 0//radio group id
        variableExists = false
        finalOne.var1.forEach { (variableType, variableInitial, varInitialStatus, variableName, variableQty, variablePrice, storeVariablePrice) ->
            if (variableQty >= 0 && variableName.trim() != "") {
                val finalVariablePrice = if (Constants.userCurrency == mProductDetails.currency)
                    storeVariablePrice
                else
                    variablePrice

                mSelectedVar1 = null//is set to null if there is a variable to be selected
                highestArray[1] =
                    if (highestArray[1] > finalVariablePrice) highestArray[1] else finalVariablePrice
                varsHeader1 = variableType
                addVariable(
                    "var1", binding.llVariationBody1, variableId, varInitialStatus, variableInitial
                )
                var1Data[variableId] = Var1(
                    variableType,
                    variableInitial,
                    varInitialStatus,
                    variableName,
                    variableQty,
                    finalVariablePrice
                )
                variableExists = true
            }
            ++variableId
        }

        if (variableId > 0) {//show variable 1 space
            binding.tvVariationTitle1.text = varsHeader1

            if (variableExists) {
                binding.tvVariationTitle1.visibility = View.VISIBLE
                binding.tvSelectedVariation1.visibility = View.VISIBLE
                binding.llVariationBody1.visibility = View.VISIBLE
            }
        }

        var varsHeader2 = ""
        variableId = 0
        variableExists = false
        finalOne.var2.forEach { (variableType, variableInitial, varInitialStatus, variableName, variableQty, variablePrice, storeVariablePrice) ->
            if (variableQty >= 0 && variableName.trim() != "") {
                val finalVariablePrice = if (Constants.userCurrency == mProductDetails.currency)
                    storeVariablePrice
                else
                    variablePrice

                mSelectedVar2 = null//is set to null if there is a variable to be selected
                highestArray[2] =
                    if (highestArray[2] > finalVariablePrice) highestArray[2] else finalVariablePrice
                varsHeader2 = variableType
                addVariable(
                    "var2", binding.llVariationBody2, variableId, varInitialStatus, variableInitial
                )
                var2Data[variableId] = Var2(
                    variableType,
                    variableInitial,
                    varInitialStatus,
                    variableName,
                    variableQty,
                    finalVariablePrice
                )
                variableExists = true
            }
            ++variableId
        }

        if (variableId > 0) {//show variable 1 space
            binding.tvVariationTitle2.text = varsHeader2

            if (variableExists) {
                binding.tvVariationTitle2.visibility = View.VISIBLE
                binding.tvSelectedVariation2.visibility = View.VISIBLE
                binding.llVariationBody2.visibility = View.VISIBLE
            }
        }

        val highestUnitPrice = highestArray.sum()
        if (highestUnitPrice > 0.0) {
            val highestPossiblePrice = mProductDetails.unit_price + highestUnitPrice

            if (highestPossiblePrice > mProductDetails.unit_price) {
                binding.tvPdtPriceSeparator.apply {
                    text = " ~ "
                    visibility = View.VISIBLE
                }
                binding.tvPdtPrice2.text =
                    CCFunctions.formatCurrency(
                        highestPossiblePrice,
                        mProductDetails.currency
                    )
            } else {
                binding.tvPdtPriceSeparator.visibility = View.GONE
            }
        }

        val itemsLeftLabel = resources.getString(R.string.pdt_items_left)
        binding.tvItemsLeft.text = "${mProductDetails.avail_qty} $itemsLeftLabel"

        binding.pbItemsLeft.progress =
            if (mProductDetails.avail_qty < 100) mProductDetails.avail_qty else 100

        binding.addToCart.setOnClickListener {
            //add to cart
            if (mSelectedVar0 == null) {//not chosen existing variable
                val errorMsg = resources.getString(R.string.please_select)
                showErrorSnackBar("$errorMsg $varsHeader0", true)
            } else if (mSelectedVar1 == null) {//not chosen existing variable
                val errorMsg = resources.getString(R.string.please_select)
                showErrorSnackBar("$errorMsg $varsHeader1", true)
            } else if (mSelectedVar2 == null) {//not chosen existing variable
                val errorMsg = resources.getString(R.string.please_select)
                showErrorSnackBar("$errorMsg $varsHeader2", true)
            } else {
                //all selectable variables selected
                //TODO CHANGE IMAGE ON DIALOG TO THAT OF PRODUCT AND PASS IT AS PARAMETER
                showCartDialog()
            }
        }
        updateTotalPrice()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun auctionProduct() {
        binding.tvCurrentBidLabel.visibility = View.VISIBLE
        binding.tilBidInput.visibility = View.VISIBLE
        binding.tvPdtPrice.visibility = View.GONE
        binding.tvCurrentBid.visibility = View.VISIBLE
        binding.btnSubmitBid.visibility = View.VISIBLE

        binding.pbItemsLeft.visibility = View.GONE
        binding.totalLabel.visibility = View.GONE
        binding.pdtTotal.visibility = View.GONE
        binding.addToCart.visibility = View.GONE


        binding.tvCurrentBid.text =
            CCFunctions.formatCurrency(
                mProductDetails.unit_price,
                mProductDetails.currency
            )

        if (Constants.userId !== "" && mProductDetails.bid_user_id?.trim() == Constants.userId.trim()) {
            binding.highestBidderNotification.visibility = View.VISIBLE
        } else {
            binding.highestBidderNotification.visibility = View.GONE
        }

        val tvAuctionTime = binding.tvAuctionTime
        val calender = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = calender.timeInMillis / 1000
        var distance = mProductDetails.promo_expiry - now

        val tickHandler = TickHandler(backgroundScope, 1000)

        var isCoroutineEnabled = true
        backgroundScope.launch {
            tickHandler.tickFlow.collectLatest {
                if (!isCoroutineEnabled) {
                    return@collectLatest  // Exit the coroutine early if the flag is set to false
                }

                val days = floor(distance.toDouble() / (60 * 60 * 24)).toInt()
                val hours = floor((distance.toDouble() % (60 * 60 * 24)) / (60 * 60)).toInt()
                val minutes = floor((distance.toDouble() % (60 * 60)) / (60)).toInt()
                val seconds = floor((distance.toDouble() % 60)).toInt()

                val outputText = if (days > 0) {
                    "${days}d : ${hours}h : ${minutes}m : ${seconds}s"
                } else if (distance > 0) {
                    "${hours}h : ${minutes}m : ${seconds}s"
                } else {//EXPIRED PROMO
                    withContext(Dispatchers.Main) {
                        binding.tilBidInput.visibility = View.GONE
                        binding.btnSubmitBid.visibility = View.GONE

                        if (mProductDetails.bid_user_id == Constants.userId) {
                            val msg = resources.getString(R.string.access_your_watchlist_on_journcart_com_to_purchase_your_bid)
                            showErrorSnackBar(msg, false)

                            binding.highestBidderMessage.apply {
                                visibility = View.VISIBLE
                                this.setOnClickListener {
                                    val url = "${Constants.url}/product/item/ $productId"
                                    val intent = Intent(requireActivity(), WebViewActivity::class.java)
                                    intent.putExtra("url",url)
                                    startActivity(intent)
                                }
                            }
                        }
                        isCoroutineEnabled = false // Disable the coroutine here
                        resources.getString(R.string.auction_expired)
                    }
                }

                --distance
                withContext(Dispatchers.Main) {
                    tvAuctionTime.text = outputText
                }
            }
        }


        val getCurrency = Constants.userCurrency
        if (getCurrency == "") {
            binding.tilBidInput.hint = resources.getString(R.string.hint_place_bid, "UGX")
        } else {
            binding.tilBidInput.hint = resources.getString(R.string.hint_place_bid, getCurrency)
        }


        val json = mProductDetails.vars_arr
        val varLevel1 = Gson().fromJson(json, List::class.java)

        var variableId = 0
        val llVariationBodyArray = arrayListOf(
            binding.llVariationBody0, binding.llVariationBody1, binding.llVariationBody2
        )

        val tvVariationTitleArray = arrayListOf(
            binding.tvVariationTitle0, binding.tvVariationTitle1, binding.tvVariationTitle2
        )
        val tvSelectedVariationArray = arrayListOf(
            binding.tvSelectedVariation0, binding.tvSelectedVariation1, binding.tvSelectedVariation2
        )

        varLevel1.forEach {
            val varLevel2 = Gson().toJson(it)
            val varLevel3 = Gson().fromJson(varLevel2, List::class.java)
            val varLevel4 = Gson().toJson(varLevel3[0])
            val varLevel5 = Gson().fromJson(varLevel4, List::class.java)

            val variableType = varLevel5[0].toString()
            val variableInitial = varLevel5[1].toString()
            val varInitialStatus = varLevel5[2].toString()
            val variableName = varLevel5[3].toString()
            //val variablePrice = varLevel5[4].toString().toDouble()

            tvVariationTitleArray[variableId].text = variableType
            tvSelectedVariationArray[variableId].text = "($variableName)"

            tvVariationTitleArray[variableId].visibility = View.VISIBLE//this was the linear guy
            tvSelectedVariationArray[variableId].visibility = View.VISIBLE
            llVariationBodyArray[variableId].visibility = View.VISIBLE

            addVariable(
                variableId.toString(),
                llVariationBodyArray[variableId],
                variableId,
                varInitialStatus,
                variableInitial
            )

            ++variableId
        }

        binding.btnSubmitBid.setOnClickListener {
            if (Constants.userId.isNotEmpty()) {
                val bidInput = binding.etBidInput.text.toString().toDouble()
                if (bidInput > mProductDetails.unit_price) {
                    Toast.makeText(
                        activity, resources.getString(R.string.please_wait), Toast.LENGTH_SHORT
                    ).show()
                    viewModel.submitAuction(bidInput)
                    binding.etBidInput.setText("")
                } else {
                    showErrorSnackBar(resources.getString(R.string.auction_low_err), true)
                }
            } else {
                showErrorSnackBar(resources.getString(R.string.login_action_error), true)
            }
        }
    }

    private fun addVariable(
        variableGroup: String,
        llVariationBody: FlowLayout,
        variableButtonID: Int,
        varInitialStatus: String,
        variableInitial: String
    ) {
        if (variableButtonID == 0) {//reset radio group values (remove them) this helps with switching to and from dark mode
            val count: Int = llVariationBody.childCount
            if (count > 0) {
                for (i in count - 1 downTo 0) {
                    val o: View = llVariationBody.getChildAt(i)
                    if (o is ImageView || o is TextView) {
                        llVariationBody.removeViewAt(i)
                    }
                }
            }
        }

        val height = resources.getDimension(R.dimen._30sdp).toInt()
        val margin = resources.getDimension(R.dimen._3sdp).toInt()

        if (varInitialStatus == "img") {
            val padding = resources.getDimension(R.dimen._1sdp).toInt()

            val variableButton = ImageView(requireContext())
            variableButton.id = variableButtonID
            variableButton.background =
                ResourcesCompat.getDrawable(resources, R.drawable.variable_selector_image, null)

            val params = LinearLayout.LayoutParams(
                height, height
            )
            val imgJson = Gson().fromJson(variableInitial, List::class.java)
            val img = Constants.storage_url + "/" + imgJson[1]

            GlideLoader(requireContext()).loadUserPicture(img, variableButton)

            params.setMargins(margin, margin, margin, margin)

            variableButton.layoutParams = params
            variableButton.setPadding(padding, padding, padding, padding)
            variableButton.tag = variableGroup
            variableButton.setOnClickListener(this)
            llVariationBody.addView(variableButton)
        } else {
            val width = LinearLayout.LayoutParams.WRAP_CONTENT
            val horizontalPadding = resources.getDimension(R.dimen._5sdp).toInt()
            val verticalPadding = resources.getDimension(R.dimen._1sdp).toInt()

            val variableButton = TextView(requireContext())
            variableButton.id = variableButtonID
            variableButton.background =
                ResourcesCompat.getDrawable(resources, R.drawable.variable_selector_text, null)
            variableButton.text = variableInitial
            variableButton.setPadding(
                horizontalPadding,
                verticalPadding,
                horizontalPadding,
                verticalPadding
            )

            val params = LinearLayout.LayoutParams(
                width, height
            )
            params.setMargins(margin, margin, margin, margin)

            variableButton.gravity = Gravity.CENTER
            variableButton.layoutParams = params
            variableButton.tag = variableGroup
            variableButton.setOnClickListener(this)
            llVariationBody.addView(variableButton)
        }
    }

    private fun updateTotalPrice() {
        val shippingTotal: Double = if (mShippingTotal > 0) {
            mShippingTotal
        } else {
            0.0
        }
        val total =
            shippingTotal + mProductDetails.unit_price + mVar0SubTotal + mVar1SubTotal + mVar2SubTotal
        binding.pdtTotal.text =
            CCFunctions.formatCurrency(total, mProductDetails.currency)
    }

    private fun productTabs() {
        val tabLayout = binding.tabLayout
        val viewPager = binding.tabsViewpager
        //tabLayout.setSelectedTabIndicatorColor(Color.WHITE)
        //tabLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.royalBlue))
        //tabLayout.tabTextColors = ContextCompat.getColorStateList(this, android.R.color.white)

        // Set different Text Color for Tabs for when are selected or not
        //tab_layout.setTabTextColors(R.color.normalTabTextColor, R.color.selectedTabTextColor)

        val bundle = Bundle()
        bundle.putParcelable("mProductDetails", mProductDetails)
        bundle.putString("productId", productId)

        val productSpecifications = ProductSpecifications()
        val productOverview = ProductOverview()
        val productReviews = ProductReviews(viewModel)

        productSpecifications.arguments = bundle
        productOverview.arguments = bundle
        productReviews.arguments = bundle

        val fragmentList = listOf(productSpecifications, productOverview, productReviews)

        val titleList = listOf(
            resources.getString(R.string.specifications),
            resources.getString(R.string.overview),
            resources.getString(R.string.reviews_)
        )

        viewPager.adapter = ProductTabsPagerAdapter(this, fragmentList)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titleList[position]
        }.attach()

    }

    private fun showShippingMethodsDialog(shippingMethods: List<ShippingDetails>) {
        mPlaceholderDialog = Dialog(requireContext())

        mPlaceholderDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mPlaceholderDialog.setCancelable(true)
        mPlaceholderDialog.setContentView(R.layout.dialog_placeholder)
        val body = mPlaceholderDialog.findViewById(R.id.dialogLinearLayout) as LinearLayout

        mPlaceholderDialog.show()
        var shippingCounter = 0

        shippingMethodIds.clear()
        shippingMethods.forEach foreach@{
            val textView = TextView(requireContext())
            textView.id = shippingCounter
            textView.text = it.shipping_name
            textView.setPadding(10, 20, 10, 20)

            val height = resources.getDimension(R.dimen._25sdp).toInt()
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height
            )
            //params.setMargins(10, 5, 5, 10)

            textView.gravity = Gravity.START
            textView.layoutParams = params
            textView.tag = "shippingMethods"
            textView.setOnClickListener(this)
            body.addView(textView)
            shippingMethodIds[shippingCounter] = it.shipping_id
            ++shippingCounter
        }
    }

    private fun showShippingRegionsDialog() {
        var start = 0
        val total = 10
        var fetchingRegions = false
        var emptyRecords = false

        val dialog = BottomSheetDialog(requireContext())

        val view = layoutInflater.inflate(R.layout.dialog_select_region, binding.CLFragmentProduct)

        val pbRegionsLoader = view.findViewById<ProgressBar>(R.id.pbRegionsLoader)
        val rvRegions = view.findViewById<RecyclerView>(R.id.rvRegions)
        val dialogPreviousRegion = view.findViewById<Button>(R.id.dialogPreviousRegion)
        val dialogNextRegion = view.findViewById<Button>(R.id.dialogNextRegion)
        val dialogPage = view.findViewById<TextView>(R.id.dialogPage)

        pbRegionsLoader.visibility = View.VISIBLE
        productShippingRegionsAdapter =
            ProductShippingRegionsAdapter(RegionClickListener {
                backgroundScope.launch {
                    val userData = Constants.mUserData.get()!!.copy(
                        region = it.region_id
                    )
                    BasicLocalStorage(requireActivity()).setUserData(userData)
                    Constants.mUserData.set(userData)
                }
                Constants.userRegion = it.region_id

                shippingArea(
                    mProductDetails.shipping,
                    mProductDetails.shipping_id,
                    mProductDetails.shipping_type,
                    mProductDetails.shipping_name,
                    mProductDetails.shipping_regions
                )
                dialog.dismiss()
            }, mProductDetails.shipping_name)

        dialog.setOnCancelListener {
            start = 0
            fetchingRegions = false
            emptyRecords = false
        }

        rvRegions.apply {
            adapter = productShippingRegionsAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        dialogPreviousRegion.setOnClickListener {
            if(!fetchingRegions){
                start = start.minus(total)
                start = if (start < 0) 0 else start

                viewModel.getShippingRegions(productId, mProductDetails.shipping_id, start, total)
                fetchingRegions = true
                pbRegionsLoader.visibility = View.VISIBLE
                it.visibility = View.INVISIBLE
            }
        }

        dialogNextRegion.setOnClickListener {
            if(!fetchingRegions){
                if(!emptyRecords){//data was returned so u can increment for more
                    /*Log.i("testing", "not empty")
                    Log.i("testing", emptyRecords.toString())*/
                    start += total
                }else{//no data

                }
                viewModel.getShippingRegions(productId, mProductDetails.shipping_id, start, total)
                fetchingRegions = true
                pbRegionsLoader.visibility = View.VISIBLE
                it.visibility = View.INVISIBLE
            }
        }

        viewModel.shippingRegionsDataStatus.observe(viewLifecycleOwner) { it ->
            it?.let {
                /*Log.i("testing --", it.toString())
                Log.i("testing ---", start.toString())*/
                fetchingRegions = false
                emptyRecords = it.isEmpty() || (start + total) >= totalShippingRegions
                productShippingRegionsAdapter.addHeaderAndSubmitList(it)
                val page: Int = (start / total) + 1
                dialogPage.text = page.toString()
                pbRegionsLoader.visibility = View.GONE
                dialogPreviousRegion.visibility = View.VISIBLE
                dialogNextRegion.visibility = View.VISIBLE
            }
        }

        dialog.setCancelable(true)
        dialog.setContentView(view)
        dialog.show()
    }

    private fun addToWishlist() {
        viewModel.addToWishlist(Constants.deviceKey, mProductDetails.url, productId)
        viewModel.wishlistDataStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    val returnWishlist = it.data!!

                    when (returnWishlist.result) {
                        1 -> {//success
                            showErrorSnackBar(resources.getString(R.string.success_wishlist), false)
                        }
                        10 -> {
                            showErrorSnackBar(
                                resources.getString(R.string.login_action_error), true
                            )
                        }
                        else -> {
                            showErrorSnackBar(
                                resources.getString(R.string.err_unexpected_error), true
                            )
                        }
                    }
                }
                is Resource.Error -> {// TODO- UPLOADS BUT STILL GETS ERROR
                    showErrorSnackBar(resources.getString(R.string.err_unexpected_error), true)
                    //showErrorSnackBar(resources.getString(R.string.err_unexpected_error),true)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    private fun showCartDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_add_to_cart, null)

        val mainImg = Gson().fromJson(mProductDetails.main_img, ArrayList::class.java)
        val imgUrl = Constants.storage_url + "/" + mainImg[2]

        val dialogImage = view.findViewById<ImageView>(R.id.dialogImage)
        GlideLoader(requireContext()).loadUserPicture(imgUrl, dialogImage)

        val pdtName = view.findViewById<TextView>(R.id.dialogProductName)
        pdtName.text = mProductDetails.pdt_name

        val dialogVariations = view.findViewById<TextView>(R.id.dialogVariations)
        var variations = ""
        if (mSelectedVar0!!.variableName != "") variations = mSelectedVar0!!.variableName
        if (mSelectedVar1!!.variableName != "") variations += " | " + mSelectedVar1!!.variableName
        if (mSelectedVar2!!.variableName != "") variations += " | " + mSelectedVar2!!.variableName

        dialogVariations.text = variations

        val dialogProductQty = view.findViewById<EditText>(R.id.dialogProductQty)
        dialogProductQty.setText(mProductDetails.min_orders.toString())

        val btnAdd = view.findViewById<Button>(R.id.DialogBtnAdd)
        btnAdd.text = resources.getString(R.string.add_to_cart)

        btnAdd.setOnClickListener {
            //TODO SEND THE REQUEST
            //["txt","Size","Medium","M","0.5418"]

            var submitVarId = ""
            val addVariations: MutableList<List<Any>> = mutableListOf()
            if (mSelectedVar0!!.variableName != "") {
                submitVarId += mSelectedVar0!!.variableType + mSelectedVar0!!.variableName
                val baseVariablePrice = CCFunctions.convertToBaseCurrency(
                    mSelectedVar0!!.variablePrice,
                    mProductDetails.currency
                )
                addVariations.add(
                    listOf(
                        mSelectedVar0!!.varInitialStatus,
                        mSelectedVar0!!.variableType,
                        mSelectedVar0!!.variableName,
                        mSelectedVar0!!.variableInitial,
                        baseVariablePrice,
                        mSelectedVar0!!.variablePrice
                    )
                )
            }

            if (mSelectedVar1!!.variableName != "") {
                submitVarId += mSelectedVar1!!.variableType + mSelectedVar1!!.variableName
                val baseVariablePrice = CCFunctions.convertToBaseCurrency(
                    mSelectedVar1!!.variablePrice,
                    mProductDetails.currency
                )
                addVariations.add(
                    listOf(
                        mSelectedVar1!!.varInitialStatus,
                        mSelectedVar1!!.variableType,
                        mSelectedVar1!!.variableName,
                        mSelectedVar1!!.variableInitial,
                        baseVariablePrice,
                        mSelectedVar1!!.variablePrice
                    )
                )
            }

            if (mSelectedVar2!!.variableName != "") {
                submitVarId += mSelectedVar2!!.variableType + mSelectedVar2!!.variableName
                val baseVariablePrice = CCFunctions.convertToBaseCurrency(
                    mSelectedVar2!!.variablePrice,
                    mProductDetails.currency
                )
                addVariations.add(
                    listOf(
                        mSelectedVar2!!.varInitialStatus,
                        mSelectedVar2!!.variableType,
                        mSelectedVar2!!.variableName,
                        mSelectedVar2!!.variableInitial,
                        baseVariablePrice,
                        mSelectedVar2!!.variablePrice
                    )
                )
            }

            if (dialogProductQty.text.toString().toInt() < mProductDetails.min_orders) {
                Toast.makeText(
                    activity, resources.getString(
                        R.string.product_minimum_quantity, mProductDetails.min_orders
                    ), Toast.LENGTH_SHORT
                ).show()
            } else if (dialogProductQty.text.toString().toInt() > mProductDetails.max_orders) {
                Toast.makeText(
                    activity, resources.getString(
                        R.string.product_maximum_quantity, mProductDetails.max_orders
                    ), Toast.LENGTH_SHORT
                ).show()
            } else {

                val submitVariations = Gson().toJson(addVariations)
                val qtyId = dialogProductQty.text.toString().toInt()
                val country = Constants.userCountry
                val regionId = Constants.userRegion

                viewModel.addToCart(
                    Constants.deviceKey,
                    submitVarId,
                    mProductDetails.url,
                    productId,
                    submitVariations,
                    mProductDetails.shipping_id,
                    qtyId,
                    country,
                    regionId
                )
                viewModel.cartDataStatus.observe(viewLifecycleOwner) {
                    when (it) {
                        is Resource.Loading -> {
                            Toast.makeText(
                                activity,
                                resources.getString(R.string.please_wait),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is Resource.Success -> {
                            val cartReturn = it.data!!
                            when (cartReturn.result) {
                                1 -> {
                                    dialog.dismiss()
                                    showErrorSnackBar(
                                        resources.getString(R.string.success_cart), false
                                    )
                                }
                                10 -> {//means the user is logged off
                                    dialog.dismiss()
                                    showErrorSnackBar(
                                        resources.getString(R.string.login_action_error), true
                                    )
                                }
                                else -> {
                                    dialog.dismiss()
                                    showErrorSnackBar(
                                        resources.getString(R.string.err_unexpected_error), true
                                    )
                                }
                            }

                        }
                        is Resource.Error -> {// TODO- UPLOADS BUT STILL GETS ERROR
                            dialog.dismiss()
                            showErrorSnackBar(
                                resources.getString(R.string.err_unexpected_error), false
                            )
                        }
                    }
                }

            }

        }
        dialog.setCancelable(true)
        dialog.setContentView(view)
        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {
        if (v != null) {
            when (v.tag) {
                /*"shippingRegion" -> {
                    val countryDialingCode = binding.ccpChangeCountry.selectedCountryCode
                    var viewId = v.id.toString()
                    if (viewId.length < 10) {//prepend the zeros so its a standard length of 10 b4 prepending the country code
                        val difference = 10 - viewId.length
                        viewId = "0".repeat(difference) + v.id
                    }
                    val regionId = countryDialingCode + viewId
                    mPlaceholderDialog.dismiss()

                    uiScope.launch {
                        val userData = Constants.mUserData.get()!!.copy(
                            region = regionId
                        )
                        BasicLocalStorage(requireActivity()).setUserData(userData)
                        Constants.mUserData.set(userData)
                    }

                    Constants.userRegion = regionId

                    shippingArea(
                        mProductDetails.shipping,
                        mProductDetails.shipping_id,
                        mProductDetails.shipping_type,
                        mProductDetails.shipping_name,
                        mProductDetails.shipping_regions,
                        mProductDetails.estimated_time_in,
                        mProductDetails.estimated_time_out
                    )
                }*/
                "shippingMethods" -> {
                    val shippingMethod = v.id
                    mPlaceholderDialog.dismiss()
                    val shippingMethodId = shippingMethodIds[shippingMethod]
                    shippingMethodId?.let {
                        viewModel.setShippingMethods(
                            Constants.deviceKey, productId, shippingMethodId
                        )

                    }
                    //TODO get shipping details based on id
                }
                "var0" -> {
                    val checkedId = v.id
                    val count: Int = binding.llVariationBody0.childCount
                    if (count > 0) {
                        for (i in count - 1 downTo 0) {
                            val o: View = binding.llVariationBody0.getChildAt(i)
                            if (o is ImageView || o is TextView) {
                                o.isActivated = o.id == checkedId
                            }
                        }
                    }
                    mSelectedVar0 = var0Data[checkedId]
                    binding.tvSelectedVariation0.text = "(${mSelectedVar0!!.variableName})"
                    mVar0SubTotal = mSelectedVar0!!.variablePrice
                    updateTotalPrice()
                }
                "var1" -> {
                    val checkedId = v.id
                    val count: Int = binding.llVariationBody1.childCount
                    if (count > 0) {
                        for (i in count - 1 downTo 0) {
                            val o: View = binding.llVariationBody1.getChildAt(i)
                            if (o is ImageView || o is TextView) {
                                o.isActivated = o.id == checkedId
                            }
                        }
                    }
                    mSelectedVar1 = var1Data[checkedId]
                    binding.tvSelectedVariation1.text = "(${mSelectedVar1!!.variableName})"
                    mVar1SubTotal = mSelectedVar1!!.variablePrice
                    updateTotalPrice()
                }
                "var2" -> {
                    val checkedId = v.id
                    val count: Int = binding.llVariationBody2.childCount
                    if (count > 0) {
                        for (i in count - 1 downTo 0) {
                            val o: View = binding.llVariationBody2.getChildAt(i)
                            if (o is ImageView || o is TextView) {
                                o.isActivated = o.id == checkedId
                            }
                        }
                    }
                    mSelectedVar2 = var2Data[checkedId]
                    binding.tvSelectedVariation2.text = "(${mSelectedVar2!!.variableName})"
                    mVar2SubTotal = mSelectedVar2!!.variablePrice
                    updateTotalPrice()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
        mainToolbar.title = productId
        mainToolbar.visibility = View.VISIBLE
        mainToolbar.logo = null

    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {//showing fragment
            val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
            mainToolbar.title = productId
            mainToolbar.logo = null
        }
        super.onHiddenChanged(hidden)
    }

    override fun onDestroyView() {
        _binding = null
        job.cancel()
        super.onDestroyView()
    }
}