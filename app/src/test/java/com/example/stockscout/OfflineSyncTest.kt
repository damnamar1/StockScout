package com.example.stockscout

import com.example.stockscout.domain.model.PendingPick
import com.example.stockscout.domain.model.SyncStatus
import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.domain.usecase.SyncPendingPicksUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OfflineSyncTest {

    private lateinit var repository: ItemRepository
    private lateinit var useCase: SyncPendingPicksUseCase

    private val pendingPick = PendingPick(
        id = 1L, itemCode = "WGT-A", newQuantity = 9,
        timestamp = 1000L, status = SyncStatus.PENDING, retryCount = 0
    )

    @Before
    fun setUp() {
        repository = mock()
        useCase = SyncPendingPicksUseCase(repository)
    }

    @Test
    fun `queued pick is claimed, sent, and marked SYNCED`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(listOf(pendingPick))
        whenever(repository.claimPickForSync(1L)).thenReturn(true)
        whenever(repository.syncPick("WGT-A", 9, 1000L)).thenReturn(Result.success(Unit))

        useCase()

        verify(repository).resetStuckSyncs()
        verify(repository).claimPickForSync(1L)
        verify(repository).syncPick("WGT-A", 9, 1000L)
        verify(repository).updatePickStatus(1L, SyncStatus.SYNCED, 0)
    }

    @Test
    fun `pick is NOT posted if claim fails (already claimed by another worker)`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(listOf(pendingPick))
        whenever(repository.claimPickForSync(1L)).thenReturn(false)

        useCase()

        verify(repository).claimPickForSync(1L)
        verify(repository, never()).syncPick(any(), any(), any())
        verify(repository, never()).updatePickStatus(any(), any(), any())
    }

    @Test
    fun `failed POST increments retry and reverts to PENDING`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(listOf(pendingPick))
        whenever(repository.claimPickForSync(1L)).thenReturn(true)
        whenever(repository.syncPick("WGT-A", 9, 1000L)).thenReturn(Result.failure(Exception("Net")))

        useCase()

        verify(repository).updatePickStatus(1L, SyncStatus.PENDING, 1)
    }

    @Test
    fun `pick marked FAILED after retry count exceeds 5`() = runTest {
        val pick = pendingPick.copy(retryCount = 5, status = SyncStatus.FAILED)
        whenever(repository.getPendingPicks()).thenReturn(listOf(pick))
        whenever(repository.claimPickForSync(1L)).thenReturn(true)
        whenever(repository.syncPick("WGT-A", 9, 1000L)).thenReturn(Result.failure(Exception()))

        useCase()

        verify(repository).updatePickStatus(1L, SyncStatus.FAILED, 6)
    }

    @Test
    fun `stuck IN_PROGRESS picks are reset on every sync run`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(emptyList())

        useCase()

        verify(repository).resetStuckSyncs()
    }

    @Test
    fun `returns true when all claimed picks sync successfully`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(listOf(pendingPick))
        whenever(repository.claimPickForSync(1L)).thenReturn(true)
        whenever(repository.syncPick("WGT-A", 9, 1000L)).thenReturn(Result.success(Unit))

        assertThat(useCase()).isTrue()
    }

    @Test
    fun `returns false when a claimed pick fails`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(listOf(pendingPick))
        whenever(repository.claimPickForSync(1L)).thenReturn(true)
        whenever(repository.syncPick("WGT-A", 9, 1000L)).thenReturn(Result.failure(Exception()))

        assertThat(useCase()).isFalse()
    }

    @Test
    fun `empty queue returns true and never claims anything`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(emptyList())

        assertThat(useCase()).isTrue()
        verify(repository, never()).claimPickForSync(any())
    }

    @Test
    fun `SYNCED rows are cleaned up after the loop on every run`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(listOf(pendingPick))
        whenever(repository.claimPickForSync(1L)).thenReturn(true)
        whenever(repository.syncPick("WGT-A", 9, 1000L)).thenReturn(Result.success(Unit))

        useCase()

        verify(repository).clearSyncedPicks()
    }

    @Test
    fun `cleanup runs even when the queue is empty`() = runTest {
        whenever(repository.getPendingPicks()).thenReturn(emptyList())

        useCase()

        verify(repository).clearSyncedPicks()
    }
}
