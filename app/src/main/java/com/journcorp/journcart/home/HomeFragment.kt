package com.journcorp.journcart.home

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cache.StoriesCacheFactory
import cache.StoriesConfig
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.journcorp.journcart.R
import com.journcorp.journcart.brands.BrandsFragment
import com.journcorp.journcart.categories.CategoriesFragment
import com.journcorp.journcart.core.activities.StoryActivity
import com.journcorp.journcart.core.adapters.*
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.utils.*
import com.journcorp.journcart.databinding.FragmentHomeBinding
import com.journcorp.journcart.events.EventsFragment
import com.journcorp.journcart.home.adapter.*
import com.journcorp.journcart.home.models.Brand
import com.journcorp.journcart.home.models.HomeTopCarousel
import com.journcorp.journcart.searchResults.SearchResultsFragment
import com.journcorp.journcart.store.StoreFragment
import com.redmadrobot.stories.models.*
import com.redmadrobot.stories.stories.StoriesController
import com.redmadrobot.stories.stories.adapter.StoriesBasePreviewAdapter
import com.redmadrobot.stories.utils.AnimationUtils
import com.redmadrobot.stories.utils.HorizontalMarginItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs


class HomeFragment : Fragment(), StoriesBasePreviewAdapter.StoriesAdapterListener,
    View.OnClickListener {
    //    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var dataSource: MainRoomDatabase
    private var screenSize = 0
    private var mSearchKeyword: HashMap<Int, String> = hashMapOf()
    private lateinit var viewModel: HomeViewModel

    private lateinit var job: Job
    private lateinit var uiScope: CoroutineScope
    private lateinit var backgroundScope: CoroutineScope

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewPagerAdapter: SliderViewPagerAdapter
    private lateinit var navBarAdapter: NavBarAdapterAdapter
    private lateinit var categoryAdapter: CatIconAdapter
    private lateinit var topSelectionAdapter: ProductAdapter
    private lateinit var auctionsAdapter: ProductAdapter
    private lateinit var storeAdAdapter: StoreAdAdapter
    private lateinit var brandAdapter: BrandAdapter
    private lateinit var productAutoLoadAdapter: ProductAutoLoadAdapter

    private var mViewPagerPosition = 0
    private var mViewPagerCounter = 0

    private lateinit var mStoryController: StoriesController

    private val onImageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            mViewPagerPosition = position
            mViewPagerCounter = 0
        }
    }

    private var mViewPagerData: List<HomeTopCarousel>? = null
    private var mBrandsData: List<Brand>? = null

    private lateinit var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener
    private lateinit var btnLoadMore: Button

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        /*setHasOptionsMenu(true)*/
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        job = Job()
        uiScope = CoroutineScope(Dispatchers.Main + job)
        backgroundScope = CoroutineScope(Dispatchers.IO + job)

        screenSize = CCFunctions.getScreenWidth(requireActivity())

        val application = requireNotNull(activity).application
        dataSource = MainRoomDatabase.getInstance(application)
        val viewModelFactory = HomeViewModelFactory(
            dataSource, screenSize, this, application
        )
        viewModel =
            ViewModelProvider(this@HomeFragment, viewModelFactory)[HomeViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        checkLiveNetworkConnection(requireActivity())

        mStoryController =
            StoriesCacheFactory.init(application, backgroundScope).preloadImages(true)
                .setConfig(StoriesConfig.All).getInstance()

        if (savedInstanceState?.getBoolean("resumed") == true) {
            productAutoLoadAdapter.refresh()
        }

        val spanCount = spanCount()

        viewPagerAdapter = SliderViewPagerAdapter(CarouselClickListener { carouselUrl ->
            Toast.makeText(context, carouselUrl, Toast.LENGTH_SHORT).show()
        })

        binding.homeTopSlider.apply {
            adapter = viewPagerAdapter
            registerOnPageChangeCallback(onImageChangeCallback)
            setPageTransformer(DepthPageTransformer())

            setOnTouchListener { _, event ->
                while (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    mViewPagerCounter = 0
                }
                true
            }
            setOnClickListener {
                mViewPagerCounter = 0
            }
        }

        navBarAdapter = NavBarAdapterAdapter(NavbarClickListener {
            addFragment(StoreFragment(it))
        }, resources.getString(R.string.navigate_stores))

        val navBarCount = if (screenSize <= 599) {
            1
        } else if (screenSize <= 899) {
            2
        } else if (screenSize <= 1299) {
            2
        } else if (screenSize <= 1699) {
            3
        } else {
            4
        }
        val navBarManager = GridLayoutManager(activity, navBarCount)
        navBarManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = when (position) {
                0 -> navBarCount
                else -> 1
            }
        }
        binding.rvNavBar.apply {
            adapter = navBarAdapter
            layoutManager = navBarManager
        }

        categoryAdapter = CatIconAdapter(CatClickListener { categoryCode ->
            addFragment(CategoriesFragment(categoryCode))
        })
        val catSpanCount = if (screenSize <= 480) {
            3
        } else if (screenSize <= 720) {
            3
        } else if (screenSize <= 1080) {
            4
        } else {
            4
        }
        val manager = GridLayoutManager(activity, catSpanCount)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = when (position) {
                0 -> catSpanCount
                else -> 1
            }
        }

        binding.rvCat.apply {
            adapter = categoryAdapter
            layoutManager = manager
        }

        topSelectionAdapter = ProductAdapter(uiScope, ProductClickListener { productId ->
            openProductDetails(productId)
        }, PeekClickListener { product ->
            productPeek(product)
        }, WishlistClickListener { productId ->
            Toast.makeText(context, "wishlist $productId", Toast.LENGTH_SHORT).show()
        }, resources.getString(R.string.top_selection)
        )
        val topManager = GridLayoutManager(activity, spanCount)
        topManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = when (position) {
                0 -> spanCount
                else -> 1
            }
        }
        binding.rvTopSelection.apply {
            adapter = topSelectionAdapter
            layoutManager = topManager
        }

        auctionsAdapter = ProductAdapter(uiScope, ProductClickListener { productId ->
            openProductDetails(productId)
        }, PeekClickListener { product ->
            productPeek(product)
        }, WishlistClickListener { productId ->
            Toast.makeText(context, "wishlist $productId", Toast.LENGTH_SHORT).show()
        }, resources.getString(R.string.featured_live_auction_deals)
        )

        val auctionsManager = GridLayoutManager(activity, spanCount)
        auctionsManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = when (position) {
                0 -> spanCount
                else -> 1
            }
        }
        binding.rvAuctions.apply {
            adapter = auctionsAdapter
            layoutManager = auctionsManager
        }

        brandAdapter = BrandAdapter(BrandClickListener { brandId ->
            addFragment(BrandsFragment(brandId))
        }, resources.getString(R.string.shop_by_brands))

        val brandManager = GridLayoutManager(
            activity, spanCount, GridLayoutManager.VERTICAL, false
        )
        brandManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = when (position) {
                0 -> spanCount
                else -> 1
            }
        }
        binding.rvBrand.apply {
            adapter = brandAdapter
            layoutManager = brandManager
        }

        productAutoLoadAdapter = ProductAutoLoadAdapter(uiScope, ProductClickListener { productId ->
            openProductDetails(productId)
        }, PeekClickListener { product ->
            productPeek(product)
        }, WishlistClickListener { productId ->
            Toast.makeText(context, "wishlist $productId", Toast.LENGTH_SHORT).show()
        })
        val autoLoadManager = GridLayoutManager(activity, spanCount)
        autoLoadManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = when (position) {
                0 -> 1
                else -> 1
            }
        }
        binding.rvAutoLoad.apply {
            adapter =
                productAutoLoadAdapter.withLoadStateHeaderAndFooter(header = ProductLoadStateAdapter {
                    productAutoLoadAdapter.retry()
                }, footer = ProductLoadStateAdapter {
                    productAutoLoadAdapter.retry()
                })
            layoutManager = autoLoadManager
        }

        // Custom story frame impl.
        val story = binding.recyclerStories
        initPreviewRecycler(story) { storiesInputParams ->
            val intent = StoryActivity.newIntent(
                context = requireContext(), storiesInputParams = storiesInputParams
            )
            openStoriesActivityAnimated(intent)
        }

        binding.brandSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.length > 3) {
                    viewModel.brandsSearch(newText)
                } else {
                    brandAdapter.addHeaderAndSubmitList(mBrandsData)
                }
                return false
            }
        })

        btnLoadMore = binding.btnLoadMore
        var clickedLoadMore = false
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            btnLoadMore.getGlobalVisibleRect(rect)

            if (rect.intersect(
                    0,
                    0,
                    btnLoadMore.rootView.width,
                    btnLoadMore.rootView.height
                ) && !clickedLoadMore
            ) {
                // Button is in view
                btnLoadMore.performClick()
                clickedLoadMore = true
            }
        }
        btnLoadMore.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        observers()
        return binding.root
    }


    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun observers() {
        uiScope.launch {
            dataSource.keywordsDAO.getAllRecords().collectLatest {
                for ((index, keyword) in it.withIndex()) {
                    mSearchKeyword[index] = keyword.title
                }
                addChips()
            }
        }

        uiScope.launch {
            viewModel.timer.collectLatest {
                mViewPagerData?.let {
                    if (mViewPagerCounter > 0 && mViewPagerCounter % 5 == 0) {//every 15 seconds
                        ++mViewPagerPosition

                        if (mViewPagerPosition >= mViewPagerData!!.size) {
                            mViewPagerPosition = 0
                        }
                        binding.homeTopSlider.setCurrentItem(mViewPagerPosition, true)
                    }
                    ++mViewPagerCounter
                }
            }
        }

        //viewpager
        uiScope.launch {
            viewModel.pagerDataStatus.collectLatest {
                mViewPagerData = it
                mViewPagerData?.let {
                    viewPagerAdapter.addHeaderAndSubmitList(mViewPagerData)
                }
            }
        }

        //stories
        uiScope.launch {
            viewModel.storiesStatus.observe(viewLifecycleOwner) {
                it?.let { it1 ->
                    //mStoryController!!.clearAndAdd(StoriesConfig.All, it1)
                    mStoryController.add(it1)
                }
            }
        }

        //navbar
        uiScope.launch {
            viewModel.navBarAdsStatus.observe(viewLifecycleOwner) {
                navBarAdapter.addHeaderAndSubmitList(it)
            }
        }

        //event ad
        uiScope.launch {
            viewModel.eventAdStatus.observe(viewLifecycleOwner) {
                var eventAdObject: Story?
                val eventAdInclude = binding.includeEventAd

                if (it.isNotEmpty()) {
                    val eventAd = it

                    eventAd?.let { evt ->
                        val eventAdData = evt[0]
                        if (eventAdData.event_name !== "") {
                            val eventImg = eventAdInclude.eventImg
                            val eventName = eventAdInclude.eventName
                            //val btnEventOpen = eventAdInclude.btnEventOpen

                            eventAdInclude.flEventFrame.visibility = View.VISIBLE

                            val img = if (screenSize <= 600) {
                                Constants.storage_url + "/" + eventAdData.media_preview
                            } else {
                                Constants.storage_url + "/" + eventAdData.media
                            }

                            context?.let { con ->
                                GlideLoader(con).loadUserPicture(img, eventImg)
                            }
                            eventName.text = eventAdData.event_name
                        }

                        val eventImages = mutableListOf<StoryFrame>()

                        val uiModeManager =
                            context?.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

                        var controlsColor = StoryFrameControlsColor.DARK
                        if (isNightMode) {
                            controlsColor = StoryFrameControlsColor.LIGHT
                        }

                        eventAd.forEach { eventAdIt ->
                            //the loop is to get all images linked to that
                            eventImages.add(
                                StoryFrame(
                                    imageUrl = Constants.storage_url + "/" + eventAdIt.media,
                                    content = StoryFrameContent(
                                        controlsColor = controlsColor, // Color for progress and close button.
                                        showGradients = StoryFrameShowGradients.BOTTOM, // Where to show gradient.
                                        position = StoryFrameContentPosition.BOTTOM, // Position of contents relative to the StoryFrame.
                                        textColor = "#FFFFFF",
                                        header1 = eventAdData.event_name,
                                        header2 = Constants.storage_url + "/" + eventAdData.media_preview,
                                        descriptions = listOf("", ""),
                                        action = StoryFrameAction(
                                            text = "event", url = eventAdData.url
                                        ),
                                        gradientColor = "#000000"
                                    )
                                )
                            )
                        }

                        eventAdObject = Story(
                            id = "eventAd",
                            name = eventAdData.event_name,
                            isSeen = false,
                            previewUrl = Constants.storage_url + "/" + eventAdData.media_preview,
                            title = eventAdData.event_name,
                            frames = eventImages
                        )
                        mStoryController.add(listOf(eventAdObject!!))
                        mStoryController.update(listOf(eventAdObject!!))

                        eventAdInclude.btnEventOpen.setOnClickListener {
                            addFragment(EventsFragment(eventAdData.url))
                        }
                    }
                }

            }
        }

        //categories
        uiScope.launch {
            viewModel.categoriesStatus.collectLatest {
                categoryAdapter.addHeaderAndSubmitList(it)
            }
        }

        //top selection
        uiScope.launch {
            viewModel.topSelectionStatus.observe(viewLifecycleOwner) {
                topSelectionAdapter.addHeaderAndSubmitList(it)
            }
        }

        //store ad
        uiScope.launch {
            viewModel.storeAd.collectLatest {
                if (it != null) {
                    val adData = it
                    storeAdAdapter = StoreAdAdapter(uiScope, StoreClickListener { url ->
                        addFragment(StoreFragment(url))
                    }, ProductClickListener { productId ->
                        openProductDetails(productId)
                    }, PeekClickListener { product ->
                        productPeek(product)
                    }, WishlistClickListener { productId ->
                        Toast.makeText(context, "wish $productId", Toast.LENGTH_SHORT).show()
                    }, adData
                    )

                    val adManager = GridLayoutManager(activity, spanCount())
                    adManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int) = when (position) {
                            0 -> spanCount()
                            else -> 1
                        }
                    }
                    binding.rvStoreAd.apply {
                        adapter = storeAdAdapter
                        layoutManager = adManager
                    }

                    viewModel.storeAdProduct.collectLatest { productSmall ->
                        productSmall?.let {
                            storeAdAdapter.addHeaderAndSubmitList(productSmall)
                        }
                    }
                }
            }
        }

        //auction
        uiScope.launch {
            viewModel.auctionsStatus.observe(viewLifecycleOwner) { it ->
                it?.let {
                    auctionsAdapter.addHeaderAndSubmitList(it)
                }
            }
        }

        //brand
        uiScope.launch {
            viewModel.brandsStatus.observe(viewLifecycleOwner) {
                mBrandsData = it
                mBrandsData?.let {
                    brandAdapter.addHeaderAndSubmitList(mBrandsData)
                }
            }
        }

        //brand search
        uiScope.launch {
            viewModel.brandsSearchStatus.collectLatest {
                val pbBrandsSearch = binding.pbBrandsSearch
                when (it) {
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        pbBrandsSearch.visibility = View.GONE
                        val brandsData = it.data
                        brandAdapter.addHeaderAndSubmitList(brandsData)
                    }
                    is Resource.Error -> {
                        pbBrandsSearch.visibility = View.GONE
                        //val searchData = it.message.toString()
                    }
                }
            }
        }

        //paged data
        uiScope.launch {
            viewModel.pagingDataFlow.collectLatest {
                productAutoLoadAdapter.submitData(viewLifecycleOwner.lifecycle, it)
            }
        }
    }

    private fun spanCount(): Int {
        //rvAutoLoad.adapter = mAutoLoadAdapter
        return CCFunctions.productSpan(screenSize)
    }

    private fun addChips() {
        if (mSearchKeyword.isEmpty()) {
            mSearchKeyword[0] = "dress"
            mSearchKeyword[1] = "dinner"
            mSearchKeyword[2] = "watch"
            mSearchKeyword[3] = "collectibles"
            mSearchKeyword[4] = "scarf"
            mSearchKeyword[5] = "crafting"
            mSearchKeyword[6] = "lego"
            mSearchKeyword[7] = "groot"
        }

        val chipGroup = binding.cgSearchKeywords
        chipGroup.removeAllViews()

        for ((i, value) in mSearchKeyword) {
            addChip(chipGroup, i, value)
        }

    }

    private fun addChip(
        chipGroup: ChipGroup, id: Int, text: String
    ) {
        if (text.trim() !== "") {
            val tag = "searchKeyword"
            val chip = Chip(requireContext())
            chip.text = text
            chip.id = id
            chip.tag = tag

            chipGroup.addView(chip)
            chip.setOnClickListener(this)
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.tag) {
                "searchKeyword" -> {
                    val keyword = mSearchKeyword[v.id]
                    keyword?.let {
                        addFragment(
                            SearchResultsFragment(
                                keyword, "all"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun initPreviewRecycler(
        recycler: RecyclerView, onStoryClicked: (StoriesInputParams) -> Unit
    ) {
        recycler.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = StoryAdapter(onStoryClicked, StoriesInputParams.createDefaults())

            val horizontalMargin =
                resources.getDimensionPixelOffset(R.dimen.stories_preview_horizontal_margin)
            val verticalMargin =
                resources.getDimensionPixelOffset(R.dimen.stories_preview_vertical_margin)

            addItemDecoration(
                HorizontalMarginItemDecoration(
                    horizontalMargin = horizontalMargin,
                    verticalMargin = verticalMargin,
                    firstMarginStart = horizontalMargin * 2,
                    lastMarginEnd = horizontalMargin * 2
                )
            )
        }
    }

    private fun openStoriesActivityAnimated(intent: Intent) {
        AnimationUtils.setExitTransition(requireActivity(), R.transition.stories_transition)
        val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity()).toBundle()
        startActivity(intent, options)
    }

    override fun onStoryClicked(storiesInputParams: StoriesInputParams) {
        val intent = StoryActivity.newIntent(
            context = requireContext(), storiesInputParams = storiesInputParams
        )
        startActivity(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        savedInstanceState?.putBoolean("resumed", true)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        Constants.storyToMainActivity["add"]?.let { hashMap ->
            hashMap["event"]?.let {
                addFragment(EventsFragment(it))
                Constants.storyToMainActivity.clear()
            }
            hashMap["store"]?.let {
                addFragment(StoreFragment(it))
                Constants.storyToMainActivity.clear()
            }
        }
        binding.btnLoadMore.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        mStoryController.clear()

        binding.homeTopSlider.apply {
            unregisterOnPageChangeCallback(onImageChangeCallback)
            adapter = null
        }

        binding.rvAuctions.adapter = null
        binding.rvBrand.adapter = null
        binding.rvCat.adapter = null
        binding.rvAutoLoad.adapter = null
        binding.rvTopSelection.adapter = null
        binding.rvStoreAd.adapter = null


        if (::globalLayoutListener.isInitialized && ::btnLoadMore.isInitialized) {
            btnLoadMore.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        }
        viewModel.clearAutoLoadData()

        _binding = null

        job.cancel()

        super.onDestroyView()
    }

    class DepthPageTransformer : ViewPager2.PageTransformer {
        private val mMinScale = 0.75f

        override fun transformPage(view: View, position: Float) {
            view.apply {
                val pageWidth = width
                when {
                    position < -1 -> { // [-Infinity,-1)
                        // This page is way off-screen to the left.
                        alpha = 0f
                    }
                    position <= 0 -> { // [-1,0]
                        // Use the default slide transition when moving to the left page
                        alpha = 1f
                        translationX = 0f
                        translationZ = 0f
                        scaleX = 1f
                        scaleY = 1f
                    }
                    position <= 1 -> { // (0,1]
                        // Fade the page out.
                        alpha = 1 - position

                        // Counteract the default slide transition
                        translationX = pageWidth * -position
                        // Move it behind the left page
                        translationZ = -1f

                        // Scale the page down (between MIN_SCALE and 1)
                        val scaleFactor = (mMinScale + (1 - mMinScale) * (1 - abs(position)))
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                    }
                    else -> { // (1,+Infinity]
                        // This page is way off-screen to the right.
                        alpha = 0f
                    }
                }
            }
        }
    }

}

