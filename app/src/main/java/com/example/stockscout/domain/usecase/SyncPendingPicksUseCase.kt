package com.example.stockscout.domain.usecase

import com.example.stockscout.domain.model.SyncStatus
import com.example.stockscout.domain.repository.ItemRepository
import javax.inject.Inject

class SyncPendingPicksUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    /**
     * Drains the pending-pick queue.
     *
     * Concurrency model:
     * 1. Any IN_PROGRESS picks from a previously crashed worker are reset to PENDING.
     * 2. For each pick, attempt an atomic claim (PENDING/FAILED → IN_PROGRESS).
     *    If the claim fails, another worker is handling it — skip.
     * 3. POST happens only on a successfully claimed pick, so a pick can never be
     *    double-posted across concurrent workers.
     *
     * Returns true if every claimed pick synced successfully.
     */
    suspend operator fun invoke(): Boolean {
        // Recover any pick left IN_PROGRESS by a crashed/killed worker.
        repository.resetStuckSyncs()

        val pending = repository.getPendingPicks()
        var allSynced = true

        for (pick in pending) {
            // Atomic compare-and-set: only one worker wins the claim.
            if (!repository.claimPickForSync(pick.id)) continue

            val result = repository.syncPick(pick.itemCode, pick.newQuantity, pick.timestamp)
            if (result.isSuccess) {
                repository.updatePickStatus(pick.id, SyncStatus.SYNCED, pick.retryCount)
            } else {
                allSynced = false
                val newRetry = pick.retryCount + 1
                val newStatus = if (newRetry > 5) SyncStatus.FAILED else SyncStatus.PENDING
                repository.updatePickStatus(pick.id, newStatus, newRetry)
            }
        }

        // Drop SYNCED rows now that the remote has them — keeps the queue compact and
        // means observePendingCount stays accurate without bookkeeping in the worker.
        repository.clearSyncedPicks()

        return allSynced
    }
}
