package com.example.stockscout.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel() {

    private val _syncState = MutableLiveData<Resource<Unit>>()
    val syncState: LiveData<Resource<Unit>> = _syncState

    fun syncItems() {
        _syncState.value = Resource.Loading
        viewModelScope.launch {
            // Drop stale FAILED rows (exhausted retries from previous runs) and SYNCED rows
            // (already on the remote). Keeps the pending queue accurate on cold start.
            repository.clearFailedPicks()
            repository.clearSyncedPicks()

            val result = repository.fetchAndSyncItems()
            _syncState.value = if (result.isSuccess) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }
}
