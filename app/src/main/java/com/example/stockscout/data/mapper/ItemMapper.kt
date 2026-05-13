package com.example.stockscout.data.mapper

import com.example.stockscout.data.local.entity.AliasEntity
import com.example.stockscout.data.local.entity.ItemEntity
import com.example.stockscout.data.local.entity.ItemWithAliases
import com.example.stockscout.data.local.entity.PendingPickEntity
import com.example.stockscout.data.remote.dto.AliasDto
import com.example.stockscout.data.remote.dto.ItemDto
import com.example.stockscout.domain.model.Alias
import com.example.stockscout.domain.model.AliasType
import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.model.PendingPick

// --- DTO → Entity (defensive: API fields are nullable) ---

/**
 * Returns null if the DTO has no usable itemCode — an item with no code can't be stored
 * (it's the primary key) or referenced by aliases. Callers should mapNotNull and skip.
 */
fun ItemDto.toEntityOrNull(): ItemEntity? {
    val code = itemCode?.takeIf { it.isNotBlank() } ?: return null
    return ItemEntity(
        itemCode = code,
        name = name ?: "Unknown",
        unitOfMeasure = unitOfMeasure ?: "EA",
        onHandQuantity = onHandQuantity ?: 0,
        remoteId = id  // mockapi.io's auto-generated id, used later for PUT /items/{id}
    )
}

/**
 * Returns null for aliases with no value — an alias with no scannable value is useless
 * for lookup. type defaults to TEXT if missing or unparseable.
 */
fun AliasDto.toEntityOrNull(itemCode: String): AliasEntity? {
    val v = value?.takeIf { it.isNotBlank() } ?: return null
    val parsedType = runCatching {
        AliasType.valueOf((type ?: "TEXT").uppercase())
    }.getOrDefault(AliasType.TEXT)
    return AliasEntity(
        itemCode = itemCode,
        type = parsedType,
        value = v
    )
}

// --- Entity → Domain ---

fun ItemEntity.toDomain(aliases: List<AliasEntity>): Item = Item(
    itemCode = itemCode,
    name = name,
    unitOfMeasure = unitOfMeasure,
    onHandQuantity = onHandQuantity,
    aliases = aliases.map { it.toDomain() }
)

fun AliasEntity.toDomain(): Alias = Alias(type = type, value = value)

fun ItemWithAliases.toDomain(): Item = Item(
    itemCode = item.itemCode,
    name = item.name,
    unitOfMeasure = item.unitOfMeasure,
    onHandQuantity = item.onHandQuantity,
    aliases = aliases.map { it.toDomain() }
)

fun PendingPickEntity.toDomain(): PendingPick = PendingPick(
    id = id,
    itemCode = itemCode,
    newQuantity = newQuantity,
    timestamp = timestamp,
    status = status,
    retryCount = retryCount
)
