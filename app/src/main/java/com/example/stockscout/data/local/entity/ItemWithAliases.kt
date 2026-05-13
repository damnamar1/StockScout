package com.example.stockscout.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation entity: an item plus all of its aliases, fetched in a single
 * @Transaction query so the join is consistent. Used by observable list queries.
 */
data class ItemWithAliases(
    @Embedded val item: ItemEntity,
    @Relation(
        parentColumn = "itemCode",
        entityColumn = "itemCode"
    )
    val aliases: List<AliasEntity>
)
