package com.example.stockscout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockscout.data.local.entity.AliasEntity

@Dao
interface AliasDao {
    @Query("SELECT * FROM aliases WHERE itemCode = :itemCode")
    suspend fun getAliasesForItem(itemCode: String): List<AliasEntity>

    @Query("SELECT * FROM aliases WHERE value = :value LIMIT 1")
    suspend fun findAliasByValue(value: String): AliasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(aliases: List<AliasEntity>)

    @Query("DELETE FROM aliases WHERE itemCode = :itemCode")
    suspend fun deleteForItem(itemCode: String)

    @Query("DELETE FROM aliases")
    suspend fun deleteAll()
}
