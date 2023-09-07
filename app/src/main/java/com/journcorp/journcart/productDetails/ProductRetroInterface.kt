package com.journcorp.journcart.productDetails

import com.journcorp.journcart.core.models.PostResult
import com.journcorp.journcart.core.models.ShippingCosts
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.productDetails.dataClass.Product
import com.journcorp.journcart.productDetails.dataClass.ReviewDetails
import com.journcorp.journcart.productDetails.dataClass.ShippingDetails
import retrofit2.Response
import retrofit2.http.*


interface ProductRetroInterface {

    @GET("product/item/{id}?platform=mobile&action=productDetails&key=${Constants.apiKey}")
    suspend fun getProductData(@Path("id") productID: String): Response<Product>

    @FormUrlEncoded
    @POST("product/item/{id}?platform=mobile&action=shippingDetails&key=${Constants.apiKey}")
    suspend fun getProductShippingData(
        @Path("id") productID: String,
        @Field("shipping_id") shippingID: String
    ): Response<List<ShippingCosts>>

    @FormUrlEncoded
    @POST("product/item/{path_id}?platform=mobile&action=addToCart&key=${Constants.apiKey}")
    suspend fun addToCart(
        @Path("path_id") path_id: String,
        @Field("deviceKey") deviceKey: String,
        @Field("variable_id") variableId: String,
        @Field("url") url: String,
        @Field("product_id") product_id: String,
        @Field("cart_vars_arr") cart_vars_arr: String,
        @Field("shipping_id") shipping_id: String,
        @Field("qty") qty: Int,
        @Field("country") country: String,
        @Field("region_id") regionId: String
    ): Response<PostResult>//1 is true 0 is false

    @FormUrlEncoded
    @POST("product/item/{path_id}?platform=mobile&action=addToWishlist&key=${Constants.apiKey}")
    suspend fun addToWishlist(
        @Path("path_id") path_id: String,
        @Field("deviceKey") deviceKey: String,
        @Field("url") url: String,
        @Field("product_id") product_id: String
    ): Response<PostResult>//1 is true 0 is false

    @FormUrlEncoded
    @POST("product/item/{path_id}?platform=mobile&action=ch_country&key=${Constants.apiKey}")
    suspend fun setUserCountry(
        @Path("path_id") path_id: String,
        @Field("deviceKey") deviceKey: String,
        @Field("country") country: String
    ): Response<ShippingDetails>

    @FormUrlEncoded
    @POST("product/item/{path_id}?platform=mobile&action=get_shipping_methods&key=${Constants.apiKey}")
    suspend fun getShippingMethods(
        @Path("path_id") path_id: String,
        @Field("deviceKey") deviceKey: String,
        @Field("shipping") shipping: String
    ): Response<List<ShippingDetails>>

    @FormUrlEncoded
    @POST("product/item/{path_id}?platform=mobile&action=set_shipping_method&key=${Constants.apiKey}")
    suspend fun setShippingMethod(
        @Path("path_id") path_id: String,
        @Field("deviceKey") deviceKey: String,
        @Field("product_id") productId: String,
        @Field("shipping_id") shippingId: String
    ): Response<ShippingDetails>

    @FormUrlEncoded
    @POST("product/item/{path_id}?platform=mobile&action=addAuction&key=${Constants.apiKey}")
    suspend fun submitAuction(
        @Path("path_id") path_id: String,
        @Field("deviceKey") deviceKey: String,
        @Field("bid_input") bidInput: Double
    ): Response<PostResult>

    @FormUrlEncoded
    @POST("product/item/{path_id}?platform=mobile&action=getReviewsList&key=${Constants.apiKey}")
    suspend fun getReviewsList(
        @Path("path_id") path_id: String,
        @Field("deviceKey") deviceKey: String,
        @Field("start") start: Int,
        @Field("total") total: Int
    ): Response<List<ReviewDetails>>
}
