package com.example.stockscout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockscout.data.local.entity.PendingPickEntity
import com.example.stockscout.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPickDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pick: PendingPickEntity): Long

    @Query("SELECT * FROM pending_picks WHERE status IN ('PENDING', 'FAILED')")
    suspend fun getPendingPicks(): List<PendingPickEntity>

    @Query("SELECT * FROM pending_picks")
    suspend fun getAllPicks(): List<PendingPickEntity>

    @Query("UPDATE pending_picks SET status = :status, retryCount = :retryCount WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SyncStatus, retryCount: Int)

    /**
     * Atomic claim: PENDING/FAILED → IN_PROGRESS. Returns rows updated (0 if another
     * worker claimed it first).
     */
    @Query("UPDATE pending_picks SET status = 'IN_PROGRESS' WHERE id = :id AND status IN ('PENDING', 'FAILED')")
    suspend fun claimForSync(id: Long): Int

    /** Recovery: any IN_PROGRESS rows left by a crashed worker → PENDING. */
    @Query("UPDATE pending_picks SET status = 'PENDING' WHERE status = 'IN_PROGRESS'")
    suspend fun resetInProgress()

    @Query("SELECT COUNT(*) FROM pending_picks WHERE status IN ('PENDING', 'IN_PROGRESS', 'FAILED')")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM pending_picks WHERE status IN ('PENDING', 'IN_PROGRESS', 'FAILED')")
    fun observePendingCount(): Flow<Int>

    /** Purges rows already pushed to the remote — no business value in keeping them. */
    @Query("DELETE FROM pending_picks WHERE status = 'SYNCED'")
    suspend fun clearSyncedPicks()

    /** Purges rows that exhausted retries. Called on app launch to discard stale failures. */
    @Query("DELETE FROM pending_picks WHERE status = 'FAILED'")
    suspend fun clearFailedPicks()
}
