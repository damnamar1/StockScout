package com.example.stockscout.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.stockscout.data.local.AppDatabase
import com.example.stockscout.data.local.dao.AliasDao
import com.example.stockscout.data.local.dao.ItemDao
import com.example.stockscout.data.local.dao.PendingPickDao
import com.example.stockscout.data.local.entity.PendingPickEntity
import com.example.stockscout.data.mapper.toDomain
import com.example.stockscout.data.mapper.toEntityOrNull
import com.example.stockscout.data.remote.ApiService
import com.example.stockscout.data.remote.dto.PickRequestDto
import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.model.PendingPick
import com.example.stockscout.domain.model.SyncStatus
import com.example.stockscout.domain.repository.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val itemDao: ItemDao,
    private val aliasDao: AliasDao,
    private val pendingPickDao: PendingPickDao,
    private val apiService: ApiService
) : ItemRepository {

    // ---------- Reactive Flows ----------

    override fun observeItems(query: String): Flow<List<Item>> {
        val source = if (query.isBlank()) {
            itemDao.observeAllItemsWithAliases()
        } else {
            itemDao.observeSearchItemsWithAliases(query)
        }
        return source.map { list -> list.map { it.toDomain() } }
    }

    override fun observePendingPickCount(): Flow<Int> =
        pendingPickDao.observePendingCount()

    // ---------- One-shot operations ----------

    override suspend fun fetchAndSyncItems(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.getItems()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }
            val dtos = response.body().orEmpty()

            val itemEntities = dtos.mapNotNull { it.toEntityOrNull() }
            val validItemCodes = itemEntities.map { it.itemCode }.toHashSet()
            val aliasEntities = dtos.flatMap { dto ->
                val code = dto.itemCode?.takeIf { it in validItemCodes } ?: return@flatMap emptyList()
                dto.aliases.orEmpty().mapNotNull { it.toEntityOrNull(code) }
            }

            db.withTransaction {
                aliasDao.deleteAll()
                itemDao.deleteAll()
                itemDao.insertAll(itemEntities)
                aliasDao.insertAll(aliasEntities)
            }
        }.onFailure { e ->
            Log.e(TAG, "fetchAndSyncItems failed: ${e.message}", e)
        }
    }

    override suspend fun getItemByCode(itemCode: String): Item? = withContext(Dispatchers.IO) {
        itemDao.getItemByCode(itemCode)?.let { entity ->
            entity.toDomain(aliasDao.getAliasesForItem(entity.itemCode))
        }
    }

    override suspend fun findItemByAliasValue(aliasValue: String): Item? = withContext(Dispatchers.IO) {
        aliasDao.findAliasByValue(aliasValue)?.let { alias ->
            itemDao.getItemByCode(alias.itemCode)?.let { entity ->
                entity.toDomain(aliasDao.getAliasesForItem(entity.itemCode))
            }
        }
    }

    override suspend fun decrementItemQuantity(itemCode: String) = withContext(Dispatchers.IO) {
        itemDao.decrementQuantity(itemCode)
    }

    override suspend fun insertPendingPick(itemCode: String, newQuantity: Int): Long =
        withContext(Dispatchers.IO) {
            pendingPickDao.insert(
                PendingPickEntity(
                    itemCode = itemCode,
                    newQuantity = newQuantity,
                    timestamp = System.currentTimeMillis(),
                    status = SyncStatus.PENDING
                )
            )
        }

    override suspend fun getPendingPicks(): List<PendingPick> = withContext(Dispatchers.IO) {
        pendingPickDao.getPendingPicks().map { it.toDomain() }
    }

    override suspend fun updatePickStatus(id: Long, status: SyncStatus, retryCount: Int) =
        withContext(Dispatchers.IO) {
            pendingPickDao.updateStatus(id, status, retryCount)
        }

    override suspend fun claimPickForSync(id: Long): Boolean = withContext(Dispatchers.IO) {
        pendingPickDao.claimForSync(id) == 1
    }

    override suspend fun resetStuckSyncs() = withContext(Dispatchers.IO) {
        pendingPickDao.resetInProgress()
    }

    override suspend fun clearSyncedPicks() = withContext(Dispatchers.IO) {
        pendingPickDao.clearSyncedPicks()
    }

    override suspend fun clearFailedPicks() = withContext(Dispatchers.IO) {
        pendingPickDao.clearFailedPicks()
    }

    /**
     * Sync flow: POST is required (the audit log), PUT is best-effort (mirrors local state to
     * remote /items). POST goes first because once it succeeds the pick is "real"; if PUT
     * fails after, we log and move on — the next full refresh will reconcile.
     */
    override suspend fun syncPick(itemCode: String, newQuantity: Int, timestamp: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1) POST /picks — REQUIRED.
                val postResp = apiService.postPick(
                    PickRequestDto(
                        itemCode = itemCode,
                        newQuantity = newQuantity,
                        timestamp = timestamp
                    )
                )
                postResp.body()?.close()
                if (!postResp.isSuccessful) {
                    throw Exception("POST /picks HTTP ${postResp.code()}")
                }

                // 2) PUT /items/{id} — best effort.
                // We use a nested runCatching but DON'T return it as the final value of the outer block.
                runCatching {
                    val itemEntity = itemDao.getItemByCode(itemCode) ?: return@runCatching
                    val remoteId = itemEntity.remoteId?.takeIf { it.isNotBlank() } ?: run {
                        Log.w(TAG, "No remoteId for $itemCode — skipping PUT /items")
                        return@runCatching
                    }
                    val aliasMaps = aliasDao.getAliasesForItem(itemCode).map {
                        mapOf("type" to it.type.name, "value" to it.value)
                    }
                    val body = mapOf<String, Any>(
                        "itemCode" to itemEntity.itemCode,
                        "name" to itemEntity.name,
                        "unitOfMeasure" to itemEntity.unitOfMeasure,
                        "onHandQuantity" to itemEntity.onHandQuantity,
                        "aliases" to aliasMaps
                    )
                    val putResp = apiService.updateItem(remoteId, body)
                    putResp.body()?.close()
                    if (!putResp.isSuccessful) {
                        Log.w(TAG, "PUT /items/$remoteId returned HTTP ${putResp.code()} — pick was still recorded")
                    }
                }.onFailure { e ->
                    Log.w(TAG, "PUT /items failed for $itemCode (pick was recorded): ${e.message}")
                }

                // Explicitly return Unit so the outer runCatching produces Result<Unit>
                Unit
            }
        }

    private companion object {
        const val TAG = "ItemRepository"
    }
}
