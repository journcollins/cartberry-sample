package com.journcorp.journcart.notifications

import android.app.Application
import androidx.databinding.ObservableBoolean
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.viewModels.BaseMainViewModel
import com.journcorp.journcart.notifications.models.Notifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class NotificationsViewModel(
    val database: MainRoomDatabase,
    app: Application
) : BaseMainViewModel(app) {

    val isLoading = ObservableBoolean()
    private val notificationsRepository = NotificationsRepository(database)

    private val _notificationsDataStatus =
        MutableStateFlow<Resource<List<Notifications>?>?>(null)
    val notificationsDataStatus = _notificationsDataStatus.asStateFlow()

    init {
        onRefresh(0)
    }

    fun onRefresh(start: Int) {
        backgroundScope.launch {
            val deviceKey = Constants.deviceKey
            isLoading.set(true)
            val result = notificationsRepository.notificationDetails(deviceKey, start)
            isLoading.set(false)
            _notificationsDataStatus.emit(result)
        }
    }
}
