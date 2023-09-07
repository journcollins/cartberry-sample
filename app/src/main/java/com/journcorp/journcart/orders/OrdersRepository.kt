package com.journcorp.journcart.orders

import androidx.lifecycle.LiveData
import androidx.paging.*
import com.journcorp.journcart.core.RetroInstance
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.remoteMediators.OrdersRemoteMediator
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.utils.safeCall
import com.journcorp.journcart.orders.models.Orders
import com.journcorp.journcart.orders.models.UploadFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

class OrdersRepository(private val datasource: MainRoomDatabase) {
    private val instance = RetroInstance.getRetroInstance()
    private val retroService = instance.create(OrdersRetroInterface::class.java)

    fun userOrders(
        deviceKey: String,
        order_id: String,
        url: String,
        order_status: String,
    ): LiveData<PagingData<Orders>> {
        val pagingSourceFactory = if (order_status != "" && url != "") {
            { datasource.ordersDAO.getAllUrlAndOrderStatus(url, order_status.toInt()) }
        } else if (order_id != "") {
            { datasource.ordersDAO.getAllOrderId(order_id) }
        } else if (url != "") {
            { datasource.ordersDAO.getAllUrl(url) }
        } else if (order_status != "") {
            { datasource.ordersDAO.getAllOrderStatus(order_status.toInt()) }
        } else {
            { datasource.ordersDAO.getAll() }
        }

        @OptIn(ExperimentalPagingApi::class)
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false,
                maxSize = 10000
            ),
            remoteMediator = OrdersRemoteMediator(
                deviceKey, order_id, url, order_status, datasource, retroService
            ),
            pagingSourceFactory = pagingSourceFactory
        ).liveData
    }

    suspend fun confirmOrder(
        deviceKey: String,
        orderId: String,
        formData: MultipartBody.Part,
        feedbackRate: Float,
        feedbackText: String
    ): Resource<UploadFeedback?> {
        return withContext(Dispatchers.Main) {
            safeCall {
                val response = retroService.uploadFeedback(
                    deviceKey,
                    orderId,
                    formData,
                    feedbackRate,
                    feedbackText
                )
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error("")
                }
            }
        }
    }

    suspend fun confirmOrder(
        deviceKey: String,
        orderId: String,
        feedbackRate: Float,
        feedbackText: String
    ): Resource<UploadFeedback?> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.uploadFeedback(
                    deviceKey,
                    orderId,
                    feedbackRate.toDouble(),
                    feedbackText
                )

                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error("")
                }
            }
        }
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 20
    }
}