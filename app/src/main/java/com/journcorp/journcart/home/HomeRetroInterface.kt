package com.journcorp.journcart.home

import com.journcorp.journcart.core.models.ProductSmall
import com.journcorp.journcart.core.models.StoreAdResponse
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.home.models.*
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface HomeRetroInterface {

    @GET("home?platform=mobile&action=carousel&key=${Constants.apiKey}")
    suspend fun getTopPager(): Response<List<HomeTopCarousel>>

    @FormUrlEncoded
    @POST("home?platform=mobile&action=getStories&key=${Constants.apiKey}")
    suspend fun getStoriesData(
        @Field("deviceKey") deviceKey: String,
        @Field("get_stories_list") get_stories_list: Int
    ): Response<StoriesNetwork>

    @FormUrlEncoded
    @POST("home?platform=mobile&action=navBarAds&key=${Constants.apiKey}")
    suspend fun getNavBarData(
        @Field("deviceKey") deviceKey: String,
        @Field("numberOfAds") numberOfAds: Int
    ): Response<List<NavBar>>

    @FormUrlEncoded
    @POST("home?platform=mobile&action=eventAd&key=${Constants.apiKey}")
    suspend fun getEventAdData(
        @Field("deviceKey") deviceKey: String
    ): Response<List<EventAd>>

    @FormUrlEncoded
    @POST("home?platform=mobile&action=topSelection&key=${Constants.apiKey}")
    suspend fun getTopSelectionData(
        @Field("deviceKey") deviceKey: String
    ): Response<List<ProductSmall>>

    @FormUrlEncoded
    @POST("home?platform=mobile&action=ad&key=${Constants.apiKey}")
    suspend fun getAdData(
        @Field("deviceKey") deviceKey: String
    ): Response<StoreAdResponse>

    @FormUrlEncoded
    @POST("home?platform=mobile&action=auctions&key=${Constants.apiKey}")
    suspend fun getAuctionsData(
        @Field("deviceKey") deviceKey: String
    ): Response<List<ProductSmall>>

    @GET("home?platform=mobile&action=brands&key=${Constants.apiKey}")
    suspend fun getBrandsData(): Response<List<Brand>>

    @FormUrlEncoded
    @POST("home?platform=mobile&action=brands&key=${Constants.apiKey}")
    suspend fun getBrandsSearchData(
        @Field("brand_search") brand_search: String
    ): Response<List<Brand>>

    @FormUrlEncoded
    @POST("home?platform=mobile&action=autoload&key=${Constants.apiKey}")
    suspend fun getAutoLoadData(
        @Field("deviceKey") deviceKey: String,
        @Field("page") page: Int
    ): Response<ProductSmallAutoLoadResponse>
}