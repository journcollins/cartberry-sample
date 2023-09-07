package com.journcorp.journcart.home

import android.app.Application
import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.*
import androidx.paging.PagingData
import com.journcorp.journcart.core.models.ProductSmall
import com.journcorp.journcart.core.models.StoreAd
import com.journcorp.journcart.core.utils.CCFunctions
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.utils.TickHandler
import com.journcorp.journcart.home.models.*
import com.redmadrobot.stories.models.Story
import kotlinx.coroutines.*
import androidx.paging.cachedIn
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.remoteMediators.RemoteContants
import com.journcorp.journcart.core.viewModels.BaseMainViewModel
import kotlinx.coroutines.flow.*


class HomeViewModel(
    datasource: MainRoomDatabase,
    private val screenWidth: Int,
    private val savedStateHandle: SavedStateHandle,
    private val app: Application
) : BaseMainViewModel(app) {
    private val homeRepository = HomeRepository(screenWidth, datasource, app.applicationContext)

    val isLoading = ObservableBoolean()

    private var tickHandler: TickHandler = TickHandler(backgroundScope, 1000)

    private val _timer = MutableSharedFlow<Boolean>()
    val timer = _timer.asSharedFlow()

    private var _pagerDataStatus = homeRepository.topPagerData
    val pagerDataStatus: Flow<List<HomeTopCarousel>> = _pagerDataStatus

    private val _storiesStatus = homeRepository.storiesData
    val storiesStatus: LiveData<List<Story>> = _storiesStatus

    private val _navBarAdsStatus = homeRepository.navBarData
    val navBarAdsStatus: LiveData<List<NavBar>> = _navBarAdsStatus

    private val _eventAdStatus = homeRepository.eventAdData
    val eventAdStatus: LiveData<List<EventAd>> = _eventAdStatus

    private val _categoriesStatus =
        MutableStateFlow<List<Category>>(mutableListOf(Category()))
    val categoriesStatus = _categoriesStatus.asStateFlow()

    private val _topSelectionStatus = homeRepository.topSelectionData
    val topSelectionStatus: LiveData<List<ProductSmall>> = _topSelectionStatus

    private val _storeAd = MutableStateFlow<StoreAd?>(null)
    val storeAd = _storeAd.asStateFlow()

    private val _storeAdProduct = MutableStateFlow<List<ProductSmall>?>(null)
    val storeAdProduct = _storeAdProduct.asStateFlow()

    private val _auctionsStatus = homeRepository.auctionsData
    val auctionsStatus: LiveData<List<ProductSmall>> = _auctionsStatus

    private val _brandsStatus = homeRepository.brandsData
    val brandsStatus: LiveData<List<Brand>> = _brandsStatus

    private val _brandsSearchStatus =
        MutableStateFlow<Resource<List<Brand>?>>(Resource.Loading())
    val brandsSearchStatus = _brandsSearchStatus.asStateFlow()


    private val currentQuery = MutableLiveData(DEFAULT_PRODUCT_TYPE)
    private val _pagingDataFlow = currentQuery.switchMap {
        homeRepository.autoLoadData(Constants.deviceKey, it)
    }.asFlow()
    val pagingDataFlow: Flow<PagingData<ProductSmall>> = _pagingDataFlow.cachedIn(viewModelScope)
    val loadMoreState = ObservableInt(2)//button that starts paging flow visibility status


    init {
        timer()

        loadPagerData()
        stories(Constants.deviceKey)
        navBarAds(Constants.deviceKey)
        eventAd(Constants.deviceKey)
        categories(app.applicationContext)
        topSelection(Constants.deviceKey)

        networkStoreAd(Constants.deviceKey, 1)
        storeAd(1)
        featuredAuctions(Constants.deviceKey)
        brands()

        backgroundScope.launch {
            delay(10000)
            isLoading.set(false)
        }
    }

    fun onRefresh(){
        isLoading.set(true)
        loadPagerData()
        stories(Constants.deviceKey)
        navBarAds(Constants.deviceKey)
        eventAd(Constants.deviceKey)
        categories(app.applicationContext)
        topSelection(Constants.deviceKey)

        networkStoreAd(Constants.deviceKey, 1)
        storeAd(1)
        featuredAuctions(Constants.deviceKey)
        brands()

        val query = currentQuery.value
        currentQuery.postValue(query)
    }

    private fun timer() {
        backgroundScope.launch {
            // Listen for tick updates
            tickHandler.tickFlow.collect {
                _timer.emit(it)
            }
        }
    }

    private fun loadPagerData() {
        backgroundScope.launch {
            homeRepository.topPagerData()
            isLoading.set(false)
        }
    }

    private fun stories(deviceKey: String) {
        backgroundScope.launch {
            homeRepository.storiesData(deviceKey, 0)
            isLoading.set(false)
        }
    }

    private fun navBarAds(deviceKey: String) {
        backgroundScope.launch {
            homeRepository.navBarData(deviceKey)
            isLoading.set(false)
        }
    }

    private fun eventAd(deviceKey: String) {
        backgroundScope.launch {
            homeRepository.eventAdData(deviceKey)
            isLoading.set(false)
        }
    }

    private fun categories(context: Context) {
        backgroundScope.launch {
            val categories = mutableListOf<Category>()
            for (i in 100..900 step 100) {
                val catString = CCFunctions.getCatString(i, context)
                val catImageName = "cat_$i"
                categories.add(Category(i, catImageName, catString))
            }
            categories.shuffle()
            val number = if(screenWidth <= 480){
                3
            }else if(screenWidth <= 720){
                3
            }else if(screenWidth <= 1080){
                4
            }else{
                4
            }
            _categoriesStatus.emit(categories.subList(0, number))
        }
    }

    private fun topSelection(deviceKey: String) {
        backgroundScope.launch {
            homeRepository.topSelectionData(deviceKey)
        }
    }

    private fun networkStoreAd(deviceKey: String, adCode: Long) {
        backgroundScope.launch {
            homeRepository.storeAdStatusData(deviceKey, adCode)
        }
    }

    fun storeAd(adCode: Long) {
        backgroundScope.launch {
            val data = homeRepository.storeAdData(adCode)
            _storeAd.emit(data.data)
            storeAdProducts(data.data?.url)
        }
    }

    private fun storeAdProducts(url: String?) {
        val total = CCFunctions.productSpan(screenWidth)
        backgroundScope.launch {
            url?.let {
                val data = homeRepository.storeAdProductsData(url,total)
                _storeAdProduct.emit(data.data)
            }
        }
    }

    private fun featuredAuctions(deviceKey: String) {
        backgroundScope.launch {
            homeRepository.auctionsData(deviceKey)

        }
    }

    private fun brands() {
        backgroundScope.launch {
            homeRepository.brandsData()
        }
    }

    fun brandsSearch(keywords: String) {
        backgroundScope.launch {
            val data = homeRepository.brandsSearchData(keywords)
            _brandsSearchStatus.emit(data)
        }
    }

    fun pagingDataFlow(productType: Int){
        currentQuery.postValue(productType)
        loadMoreState.set(1)//hides button
    }

    fun clearAutoLoadData(){
        backgroundScope.launch {
            homeRepository.clearAutoLoadData()
        }
    }

    override fun onCleared() {
        clearAutoLoadData()
        super.onCleared()
    }

    companion object{
        private const val DEFAULT_PRODUCT_TYPE = 99
    }
}