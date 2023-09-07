package com.journcorp.journcart.notifications

import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.RetroInstance
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.utils.safeCall
import com.journcorp.journcart.notifications.models.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationsRepository(private val datasource: MainRoomDatabase) {

    private val instance = RetroInstance.getRetroInstance()
    private val retroService = instance.create(NotificationsRetroInterface::class.java)

    suspend fun notificationDetails(deviceKey: String, start: Int): Resource<List<Notifications>?> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.getNotificationData(deviceKey, start)
                if (response.isSuccessful) {
                    val body = response.body()

                    Resource.Success(body)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }
}