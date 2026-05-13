package com.example.stockscout.domain.usecase

import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.utils.Resource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PickItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    // Serializes pick operations across the app so a rapid double-tap can't race past
    // the qty<=0 guard. The use case is a @Singleton so this mutex is process-wide.
    private val mutex = Mutex()

    /**
     * Atomically validates and applies a pick:
     *  1. Re-reads the item from Room (not the caller's stale snapshot)
     *  2. Refuses if the row is missing or onHandQuantity <= 0
     *  3. Decrements onHandQuantity via SQL UPDATE (floored at 0 at the DAO level)
     *  4. Inserts a PENDING pick row for WorkManager to sync
     *
     * Returns Resource.Success on success, Resource.Error with a user-readable
     * message otherwise. Never throws into the caller.
     */
    suspend operator fun invoke(itemCode: String): Resource<Unit> = mutex.withLock {
        val item = repository.getItemByCode(itemCode)
            ?: return@withLock Resource.Error("Item not found")

        if (item.onHandQuantity <= 0) {
            return@withLock Resource.Error("Cannot pick — no stock available")
        }

        val newQuantity = item.onHandQuantity - 1   // 0 is a valid result
        repository.decrementItemQuantity(itemCode)
        repository.insertPendingPick(itemCode, newQuantity)
        Resource.Success(Unit)
    }
}
