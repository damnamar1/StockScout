package com.example.stockscout.utils

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

@OptIn(FlowPreview::class)
fun <T> Flow<T>.asLiveDataDebounced(
    scope: CoroutineScope,
    debounceMs: Long = Constants.SEARCH_DEBOUNCE_MS
): LiveData<T> {
    val liveData = MutableLiveData<T>()
    this.debounce(debounceMs)
        .onEach { liveData.postValue(it) }
        .launchIn(scope)
    return liveData
}
