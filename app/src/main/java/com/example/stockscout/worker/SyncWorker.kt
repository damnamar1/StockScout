package com.example.stockscout.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stockscout.domain.usecase.SyncPendingPicksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncPendingPicksUseCase: SyncPendingPicksUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val allSynced = syncPendingPicksUseCase()
            if (allSynced) Result.success() else Result.retry()
        } catch (e: IllegalArgumentException) {
            // Kotlin-generated check for "Parameter specified as non-null is null" — typically
            // a malformed API payload that bypassed our DTO nullability. Retry; transient.
            Log.e(TAG, "Null parameter in sync payload: ${e.message}", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "SyncWorker"
        private const val TAG = "SyncWorker"
    }
}
