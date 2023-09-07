package com.journcorp.journcart.orders

import android.app.Application
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.viewModels.BaseMainViewModel
import com.journcorp.journcart.orders.models.Orders
import com.journcorp.journcart.orders.models.UploadFeedback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

class OrdersViewModel(
    database: MainRoomDatabase,
    app: Application
) : BaseMainViewModel(app) {

    private val ordersRepository = OrdersRepository(database)

    val isLoading = ObservableBoolean()
    val orderId = ObservableField("")
    val orderUrl = ObservableField("")
    private val orderStatus = ObservableField("")

    private val currentQuery = MutableLiveData(FILTERS)
    private val _pagingDataFlow = currentQuery.switchMap {
        ordersRepository.userOrders(
            Constants.deviceKey,
            it["order_id"]!!,
            it["url"]!!,
            it["order_status"]!!
        )
    }.asFlow()
    val pagingDataFlow: Flow<PagingData<Orders>> = _pagingDataFlow.cachedIn(viewModelScope)

    private val _confirmOrderStatus =
        MutableStateFlow<Resource<UploadFeedback?>>(Resource.Loading())
    val confirmOrderStatus = _confirmOrderStatus.asStateFlow()

    init {
        onRefresh()
    }

    fun onRefresh() {
        backgroundScope.launch {
            isLoading.set(true)
            val query = currentQuery.value
            currentQuery.postValue(query)
            isLoading.set(false)
        }
    }

    fun onFilter() {
        if (orderStatus.get()?.trim() != "" && orderUrl.get()?.trim() != "") {
            filterByUrlAndOrderStatus(orderUrl.get()!!, orderStatus.get()!!.toInt())
        } else if (orderId.get()?.trim() != "") {
            filterByOrderId(orderId.get()!!)
        } else if (orderUrl.get()?.trim() != "") {
            filterByUrl(orderUrl.get()!!)
        } else if (orderStatus.get()?.trim() != "") {
            filterByOrderStatus(orderStatus.get()!!.toInt())
        } else {
            val currentHashMap = currentQuery.value
            currentHashMap?.let {
                currentHashMap["order_id"] = FILTERS["order_id"]!!
                currentHashMap["url"] = FILTERS["url"]!!
                currentHashMap["order_status"] = FILTERS["order_id"]!!
                currentQuery.postValue(currentHashMap)
            }
        }
    }

    private fun filterByOrderId(OrderId: String) {
        isLoading.set(true)
        val currentHashMap = currentQuery.value

        currentHashMap?.let {
            currentHashMap["order_id"] = OrderId
            currentQuery.postValue(currentHashMap)
        }
        isLoading.set(false)
    }

    private fun filterByUrl(url: String) {
        isLoading.set(true)
        val currentHashMap = currentQuery.value

        currentHashMap?.let {
            currentHashMap["url"] = url
            currentQuery.postValue(currentHashMap)
        }
        isLoading.set(false)
    }

    private fun filterByOrderStatus(orderStatus: Int = 0) {
        isLoading.set(true)
        val currentHashMap = currentQuery.value

        currentHashMap?.let {
            currentHashMap["order_status"] = orderStatus.toString()
            currentQuery.postValue(currentHashMap)
        }
        isLoading.set(false)
    }

    fun filterByOrderStatus(parent: Any, view: View, pos: Int, id: Long) {//called from xml
        val currentHashMap = currentQuery.value

        currentHashMap?.let {
            //1-pending, 2-processing, 3-shipped,4-complete,0 cancelled, 5-dispute
            val result = when (pos) {
                5 -> "0"
                1, 2, 3, 4 -> pos.toString()
                else -> ""
            }
            orderStatus.set(result)
            onFilter()
        }
    }

    private fun filterByUrlAndOrderStatus(url: String, orderStatus: Int) {
        isLoading.set(true)
        val currentHashMap = currentQuery.value

        currentHashMap?.let {
            currentHashMap["url"] = url
            currentHashMap["order_status"] = orderStatus.toString()
            currentQuery.postValue(currentHashMap)
        }
        isLoading.set(false)
    }

    fun confirmOrder(
        deviceKey: String,
        orderId: String,
        formData: MultipartBody.Part,
        feedbackRate: Float,
        feedbackText: String
    ) {
        uiScope.launch {
            val data = ordersRepository.confirmOrder(
                deviceKey,
                orderId,
                formData,
                feedbackRate,
                feedbackText
            )
            _confirmOrderStatus.emit(data)
        }
    }

    fun confirmOrder(
        deviceKey: String,
        orderId: String,
        feedbackRate: Float,
        feedbackText: String
    ) {
        uiScope.launch {
            val data = ordersRepository.confirmOrder(
                deviceKey,
                orderId,
                feedbackRate,
                feedbackText
            )
            _confirmOrderStatus.emit(data)
        }
    }

    companion object {
        private val FILTERS = hashMapOf("order_id" to "", "url" to "", "order_status" to "")
    }
}
