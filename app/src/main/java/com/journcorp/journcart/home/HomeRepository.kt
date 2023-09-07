package com.journcorp.journcart.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.paging.*
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.RetroInstance
import com.journcorp.journcart.core.entities.KeywordsEntity
import com.journcorp.journcart.core.entities.asDatabaseModel
import com.journcorp.journcart.core.entities.asDomainModel
import com.journcorp.journcart.core.models.ProductSmall
import com.journcorp.journcart.core.models.StoreAd
import com.journcorp.journcart.core.remoteMediators.ProductRemoteMediator
import com.journcorp.journcart.core.utils.CCFunctions
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.utils.safeCall
import com.journcorp.journcart.core.utils.safeCallBoolean
import com.journcorp.journcart.home.models.Brand
import com.journcorp.journcart.home.models.EventAd
import com.journcorp.journcart.home.models.HomeTopCarousel
import com.journcorp.journcart.home.models.NavBar
import com.redmadrobot.stories.models.Story
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HomeRepository(
    screenWidth: Int,
    private val datasource: MainRoomDatabase,
    private val context: Context? = null
) {
    private val instance = RetroInstance.getRetroInstance()
    private val retroService = instance.create(HomeRetroInterface::class.java)

    private val keywordsDAO = datasource.keywordsDAO
    private val homeTopCarouselDAO = datasource.homeTopCarouselDAO
    private val homeStoryDAO = datasource.storyDAO
    private val navBarDAO = datasource.navBarDAO
    private val eventAdDAO = datasource.eventAdDAO
    private val productSmallDAO = datasource.productSmallDAO
    private val storeAdDAO = datasource.storeAdDAO
    private val brandDAO = datasource.brandDAO

    val topPagerData: Flow<List<HomeTopCarousel>> = homeTopCarouselDAO.getAll().map {
        it.asDomainModel()
    }.asFlow()

    val storiesData: LiveData<List<Story>> = homeStoryDAO.getAll().map {
        it.asDomainModel(context)
    }


    private val navBarNumber = CCFunctions.productSpan(screenWidth)

    val navBarData: LiveData<List<NavBar>> = navBarDAO.getAll(navBarNumber).map {
        it.asDomainModel()
    }

    val eventAdData: LiveData<List<EventAd>> = eventAdDAO.getAll().map {
        it.asDomainModel()
    }

    private val productNumber: Long = 9

    val topSelectionData: LiveData<List<ProductSmall>> =
        productSmallDAO.getAllTopSelection(productNumber).map {
            it.asDomainModel()
        }

    suspend fun storeAdData(adCode: Long): Resource<StoreAd?> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val response = storeAdDAO.getAll(adCode)
                Resource.Success(response.asDomainModel())
            }
        }
    }

    suspend fun storeAdProductsData(
        adProductUrl: String, total: Int
    ): Resource<List<ProductSmall>> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val response = productSmallDAO.getAllAd(adProductUrl, total)
                Resource.Success(response.asDomainModel())
            }
        }
    }

    val auctionsData: LiveData<List<ProductSmall>> =
        productSmallDAO.getAllTopAuctions(productNumber).map {
            it.asDomainModel()
        }

    val brandsData: LiveData<List<Brand>> = brandDAO.getAll().map {
        it.asDomainModel()
    }

    suspend fun topPagerData(): Boolean {

        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response = retroService.getTopPager()
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        homeTopCarouselDAO.insert(*body.asDatabaseModel())
                    }
                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun storiesData(deviceKey: String, getStoriesList: Int): Boolean {

        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response = retroService.getStoriesData(deviceKey, getStoriesList)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {

                        val rows = body.stories.asDatabaseModel()
                        homeStoryDAO.insert(*rows)
                    }
                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun navBarData(deviceKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response = retroService.getNavBarData(deviceKey, navBarNumber)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val rows = body.asDatabaseModel()
                        navBarDAO.clear()
                        navBarDAO.insert(*rows)
                    }
                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun eventAdData(deviceKey: String): Boolean {

        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response = retroService.getEventAdData(deviceKey)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val rows = body.asDatabaseModel()
                        eventAdDAO.clear()
                        eventAdDAO.insert(*rows)
                    }
                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun topSelectionData(deviceKey: String): Boolean {

        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response =
                    retroService.getTopSelectionData(deviceKey)//TODO PICK PRODUCTS ACCORDING TO SCREEN SIZE
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val rows = body.asDatabaseModel(1)

                        val keywords = rows.map {
                            KeywordsEntity(it.keywords1)
                        }.toTypedArray()

                        keywordsDAO.deleteAll()
                        keywordsDAO.insert(*keywords)
                        productSmallDAO.insert(*rows)
                    }
                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun storeAdStatusData(deviceKey: String, adCode: Long): Boolean {
        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response =
                    retroService.getAdData(deviceKey)//TODO PICK PRODUCTS ACCORDING TO SCREEN SIZE

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val productStart = adCode + 1000

                        val storeRow = body.store_details.asDatabaseModel(adCode)
                        val productRows = body.store_products.asDatabaseModel(productStart)

                        storeAdDAO.insert(storeRow)
                        productSmallDAO.insert(*productRows)
                    }
                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun auctionsData(deviceKey: String) {
        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response = retroService.getAuctionsData(deviceKey)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val rows = body.asDatabaseModel(11)
                        val keywords = rows.map {
                            KeywordsEntity(it.keywords1)
                        }.toTypedArray()

                        keywordsDAO.insert(*keywords)
                        productSmallDAO.insert(*rows)
                    }
                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun brandsData() {
        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response = retroService.getBrandsData()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val rows = body.asDatabaseModel()
                        brandDAO.clear()
                        brandDAO.insert(*rows)
                    }
                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun brandsSearchData(keywords: String): Resource<List<Brand>?> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.getBrandsSearchData(keywords)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val rows = body.asDatabaseModel()
                        brandDAO.clear()
                        brandDAO.insert(*rows)
                    }
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

    fun autoLoadData(deviceKey: String, productType: Int): LiveData<PagingData<ProductSmall>> {

        val pagingSourceFactory = {
            datasource.productSmallDAO.getAutoLoad(
                10000, 19999
            )
        }//values match the Product Remote Mediator

        @OptIn(ExperimentalPagingApi::class) return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false, maxSize = 10000
            ), remoteMediator = ProductRemoteMediator(
                deviceKey, productType, datasource, retroService
            ), pagingSourceFactory = pagingSourceFactory
        ).liveData
    }

    suspend fun clearAutoLoadData(){
        return withContext(Dispatchers.IO) {
            datasource.productSmallDAO.clearAutoLoad(10000)
            Log.i("testing", "clearAutoLoadData()")
        }
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 20
    }
}