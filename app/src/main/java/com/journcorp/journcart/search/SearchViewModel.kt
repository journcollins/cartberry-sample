package com.journcorp.journcart.search

import androidx.lifecycle.*
import com.journcorp.journcart.core.entities.Search
import kotlinx.coroutines.launch

class SearchViewModel (private val repository: SearchRepository) : ViewModel(){
    fun insert(search: Search) = viewModelScope.launch {
        repository.insertHistory(search)
    }

    fun delete(search: Search) = viewModelScope.launch {
        repository.deleteHistory(search)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAllHistory()
    }

    val allSearchList: LiveData<List<Search>> = repository.getAllRecords.asLiveData()

    fun searchRows(search: String) : LiveData<List<Search>> = repository.searchRows(search).asLiveData()

}
