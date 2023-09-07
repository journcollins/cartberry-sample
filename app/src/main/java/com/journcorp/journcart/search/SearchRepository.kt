package com.journcorp.journcart.search

import androidx.annotation.WorkerThread
import com.journcorp.journcart.core.dao.SearchDAO
import com.journcorp.journcart.core.entities.Search
import kotlinx.coroutines.flow.Flow

class SearchRepository(private val searchDao: SearchDAO) {
    @WorkerThread
    suspend fun insertHistory(search: Search) {
        searchDao.insertHistory(search)
    }

    @WorkerThread
    suspend fun deleteHistory(search: Search) {
        searchDao.deleteHistory(search)
    }

    @WorkerThread
    suspend fun deleteAllHistory() {
        searchDao.deleteAllHistory()
    }

    val getAllRecords: Flow<List<Search>> = searchDao.getAllRecords()

    fun searchRows(search: String): Flow<List<Search>> = searchDao.searchRows(search)

}