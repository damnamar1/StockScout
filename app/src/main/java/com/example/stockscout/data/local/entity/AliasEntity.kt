package com.example.stockscout.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.stockscout.domain.model.AliasType

@Entity(
    tableName = "aliases",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["itemCode"],
            childColumns = ["itemCode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["value"]),
        Index(value = ["itemCode"])
    ]
)
data class AliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemCode: String,
    val type: AliasType,
    val value: String
)
