package com.journcorp.journcart.orders

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.journcorp.journcart.core.databases.MainRoomDatabase

class OrdersViewModelFactory(
    private val database: MainRoomDatabase, private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return OrdersViewModel(database, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}