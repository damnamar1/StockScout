package com.example.stockscout.domain.model

data class PendingPick(
    val id: Long,
    val itemCode: String,
    val newQuantity: Int,
    val timestamp: Long,
    val status: SyncStatus,
    val retryCount: Int
)

enum class SyncStatus { PENDING, IN_PROGRESS, SYNCED, FAILED }
