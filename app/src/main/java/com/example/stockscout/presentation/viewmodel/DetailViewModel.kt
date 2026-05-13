package com.example.stockscout.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.model.PendingPick
import com.example.stockscout.domain.model.SyncStatus
import com.example.stockscout.domain.usecase.PickItemUseCase
import com.example.stockscout.domain.usecase.ResolveAliasUseCase
import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.utils.Constants
import com.example.stockscout.utils.Resource
import com.example.stockscout.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val resolveAliasUseCase: ResolveAliasUseCase,
    private val pickItemUseCase: PickItemUseCase,
    private val repository: ItemRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _item = MutableLiveData<Resource<Item?>>()
    val item: LiveData<Resource<Item?>> = _item

    private val _pickState = MutableLiveData<Resource<Unit>>()
    val pickState: LiveData<Resource<Unit>> = _pickState

    private val _pendingPicks = MutableLiveData<List<PendingPick>>()
    val pendingPicks: LiveData<List<PendingPick>> = _pendingPicks

    fun resolveInput(input: String) {
        _item.value = Resource.Loading
        viewModelScope.launch {
            runCatching { resolveAliasUseCase(input) }
                .onSuccess { _item.value = Resource.Success(it) }
                .onFailure { _item.value = Resource.Error(it.message ?: "Resolution failed") }
        }
    }

    fun pick() {
        val current = (_item.value as? Resource.Success)?.data ?: return
        // Defense-in-depth: refuse even before hitting the use case so the loading
        // flicker doesn't appear when the user shouldn't be able to click anyway.
        if (current.onHandQuantity <= 0) {
            _pickState.value = Resource.Error("Cannot pick — no stock available")
            return
        }
        _pickState.value = Resource.Loading
        viewModelScope.launch {
            when (val result = pickItemUseCase(current.itemCode)) {
                is Resource.Success -> {
                    enqueueSyncWork()
                    // Re-read so the UI shows the fresh quantity and the Pick button
                    // re-evaluates its enabled state (disabled when qty hits 0).
                    val refreshed = resolveAliasUseCase(current.itemCode)
                    _item.value = Resource.Success(refreshed)
                    _pickState.value = Resource.Success(Unit)
                    loadPendingPicks()
                }
                is Resource.Error -> _pickState.value = result
                is Resource.Loading -> { /* unreachable — use case never returns Loading */ }
            }
        }
    }

    fun loadPendingPicks() {
        viewModelScope.launch {
            _pendingPicks.value = repository.getPendingPicks()
        }
    }

    private fun enqueueSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // One-time immediate sync
        val oneTime = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(Constants.SYNC_WORK_TAG)
            .build()
        workManager.enqueue(oneTime)

        // Periodic safety net (every 15 minutes)
        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(Constants.PERIODIC_SYNC_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            Constants.PERIODIC_SYNC_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
    }
}
