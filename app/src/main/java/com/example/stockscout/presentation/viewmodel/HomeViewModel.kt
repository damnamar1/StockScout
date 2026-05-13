package com.example.stockscout.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.utils.Constants
import com.example.stockscout.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    /**
     * Reactive items stream:
     *  - user types → searchQueryFlow updates
     *  - debounced (no delay for initial empty query, 300ms for typed queries)
     *  - flatMapLatest re-subscribes to Room's Flow with the new query
     *  - Room emits whenever items/aliases table changes (e.g. after a Pick)
     *  - emits Resource.Success wrapping the latest list
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val items: LiveData<Resource<List<Item>>> = searchQueryFlow
        .debounce { query -> if (query.isEmpty()) 0L else Constants.SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            repository.observeItems(query)
                .map<List<Item>, Resource<List<Item>>> { Resource.Success(it) }
                .onStart { emit(Resource.Loading) }
                .catch { e -> emit(Resource.Error(e.message ?: "Failed to load items")) }
        }
        .asLiveData()

    /** Reactive pending-pick count — auto-updates whenever the pending_picks table changes. */
    val pendingCount: LiveData<Int> = repository.observePendingPickCount().asLiveData()

    fun onSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    /** Pull-to-refresh: triggers a remote re-sync. Room Flow auto-emits the new rows. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.fetchAndSyncItems()
            _isRefreshing.value = false
        }
    }
}
