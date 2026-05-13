package com.example.stockscout.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stockscout.domain.model.SyncStatus

@Entity(tableName = "pending_picks")
data class PendingPickEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemCode: String,
    val newQuantity: Int,
    val timestamp: Long,
    val status: SyncStatus,
    val retryCount: Int = 0
)
