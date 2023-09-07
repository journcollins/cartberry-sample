package com.journcorp.journcart.orders

import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.orders.models.OrdersResult
import com.journcorp.journcart.orders.models.UploadFeedback
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface OrdersRetroInterface {
    @FormUrlEncoded
    @POST("lists/myorders?platform=mobile&action=getOrders&key=${Constants.apiKey}")
    suspend fun getOrderData(
        @Field("deviceKey") deviceKey: String,
        @Field("page") page: Int,
        @Field("order_number") orderNumber: String,
        @Field("url") url: String,
        @Field("order_status") orderStatus: String,
    ): Response<OrdersResult>

    @Multipart
    @POST("lists/myorders?platform=mobile&action=confirmOrder&key=${Constants.apiKey}")
    suspend fun uploadFeedback(
        @Part("deviceKey") deviceKey: String,
        @Part("orderId") orderId: String,
        @Part image: MultipartBody.Part,
        @Part("feedbackRate") feedbackRate: Float,
        @Part("feedbackText") feedbackText: String
    ): Response<UploadFeedback>


    @FormUrlEncoded
    @POST("lists/myorders?platform=mobile&action=confirmOrder&key=${Constants.apiKey}")
    suspend fun uploadFeedback(
        @Field("deviceKey") deviceKey: String,
        @Field("orderId") orderId: String,
        @Field("feedbackRate") feedbackRate: Double,
        @Field("feedbackText") feedbackText: String,
    ): Response<UploadFeedback>
}