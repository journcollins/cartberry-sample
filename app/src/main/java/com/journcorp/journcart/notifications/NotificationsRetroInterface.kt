package com.journcorp.journcart.notifications

import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.notifications.models.Notifications
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface NotificationsRetroInterface {
    @FormUrlEncoded
    @POST("home?platform=mobile&action=notifications&key=${Constants.apiKey}")
    suspend fun getNotificationData(
        @Field("deviceKey") deviceKey: String,
        @Field("start") start: Int
    ): Response<List<Notifications>>

}