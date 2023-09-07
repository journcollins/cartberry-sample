package com.journcorp.journcart.productDetails

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.journcorp.journcart.core.databases.MainRoomDatabase

class ProductViewModelFactory(
    private val dataSource: MainRoomDatabase,
    private val application: Application,
    private val productId: String,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ProductViewModel(
                dataSource,
                application,
                productId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}