package com.example.stockscout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.stockscout.data.local.entity.ItemEntity
import com.example.stockscout.data.local.entity.ItemWithAliases
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // ---------- Observable queries (Flow) — Room auto-emits on table changes ----------

    @Transaction
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun observeAllItemsWithAliases(): Flow<List<ItemWithAliases>>

    @Transaction
    @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' OR itemCode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun observeSearchItemsWithAliases(query: String): Flow<List<ItemWithAliases>>

    // ---------- One-shot suspend queries ----------

    @Query("SELECT * FROM items WHERE itemCode = :itemCode LIMIT 1")
    suspend fun getItemByCode(itemCode: String): ItemEntity?

    @Query("SELECT remoteId FROM items WHERE itemCode = :itemCode LIMIT 1")
    suspend fun getRemoteId(itemCode: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    /**
     * Relative decrement: safer than read-modify-write because the SQL engine reads
     * the current row atomically. Floors at 0 via MAX().
     */
    @Query("UPDATE items SET onHandQuantity = MAX(onHandQuantity - 1, 0) WHERE itemCode = :itemCode")
    suspend fun decrementQuantity(itemCode: String)

    @Query("DELETE FROM items")
    suspend fun deleteAll()
}
