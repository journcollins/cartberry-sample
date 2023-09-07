package com.journcorp.journcart.home

import android.app.Application
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.journcorp.journcart.core.databases.MainRoomDatabase

class HomeViewModelFactory(
    private val dataSource: MainRoomDatabase,
    private val screenWidth: Int,
    owner: SavedStateRegistryOwner,
    private val app: Application
) : AbstractSavedStateViewModelFactory(owner, null) {

    override fun <T : ViewModel> create(
        key: String, modelClass: Class<T>, handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return HomeViewModel(
                dataSource,
                screenWidth,
                handle,
                app
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
