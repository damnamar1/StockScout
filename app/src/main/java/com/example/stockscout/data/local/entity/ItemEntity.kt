package com.example.stockscout.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val itemCode: String,
    val name: String,
    val unitOfMeasure: String,
    val onHandQuantity: Int,
    /**
     * mockapi.io auto-generated id. Captured from GET /items so we can PUT /items/{remoteId}
     * to keep the remote on-hand quantity in sync after a local pick. Null if the row was
     * created before we started capturing it (legacy) — caller must handle.
     */
    val remoteId: String? = null
)
