package com.example.stockscout.domain.repository

import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.model.PendingPick
import com.example.stockscout.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface ItemRepository {

    // ---------- Reactive (Room-backed Flows) — emit on every table change ----------

    /** Observe items filtered by [query]. Empty query returns all items. */
    fun observeItems(query: String): Flow<List<Item>>

    /** Observe the count of picks awaiting sync (PENDING + IN_PROGRESS + FAILED). */
    fun observePendingPickCount(): Flow<Int>

    // ---------- One-shot operations ----------

    suspend fun fetchAndSyncItems(): Result<Unit>
    suspend fun getItemByCode(itemCode: String): Item?
    suspend fun findItemByAliasValue(aliasValue: String): Item?

    /** Atomically decrements onHandQuantity by 1 (floors at 0) in the items table. */
    suspend fun decrementItemQuantity(itemCode: String)

    suspend fun insertPendingPick(itemCode: String, newQuantity: Int): Long
    suspend fun getPendingPicks(): List<PendingPick>
    suspend fun updatePickStatus(id: Long, status: SyncStatus, retryCount: Int)
    suspend fun syncPick(itemCode: String, newQuantity: Int, timestamp: Long): Result<Unit>

    /** Atomically transitions a PENDING/FAILED pick to IN_PROGRESS. Returns true if successful. */
    suspend fun claimPickForSync(id: Long): Boolean

    /** Resets any stuck IN_PROGRESS picks back to PENDING (worker crashed mid-sync). */
    suspend fun resetStuckSyncs()

    /** Deletes SYNCED pick rows — they're already on the remote and serve no purpose locally. */
    suspend fun clearSyncedPicks()

    /** Deletes FAILED pick rows — exhausted retries; called on app launch to drop stale failures. */
    suspend fun clearFailedPicks()
}
