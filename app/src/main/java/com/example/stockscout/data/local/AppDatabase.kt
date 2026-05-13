package com.example.stockscout.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.stockscout.data.local.converter.Converters
import com.example.stockscout.data.local.dao.AliasDao
import com.example.stockscout.data.local.dao.ItemDao
import com.example.stockscout.data.local.dao.PendingPickDao
import com.example.stockscout.data.local.entity.AliasEntity
import com.example.stockscout.data.local.entity.ItemEntity
import com.example.stockscout.data.local.entity.PendingPickEntity

@Database(
    entities = [ItemEntity::class, AliasEntity::class, PendingPickEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun aliasDao(): AliasDao
    abstract fun pendingPickDao(): PendingPickDao
}
