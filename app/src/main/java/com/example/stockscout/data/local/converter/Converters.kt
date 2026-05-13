package com.example.stockscout.data.local.converter

import androidx.room.TypeConverter
import com.example.stockscout.domain.model.AliasType
import com.example.stockscout.domain.model.SyncStatus

class Converters {
    @TypeConverter fun aliasTypeToString(value: AliasType): String = value.name
    @TypeConverter fun stringToAliasType(value: String): AliasType = AliasType.valueOf(value)

    @TypeConverter fun syncStatusToString(value: SyncStatus): String = value.name
    @TypeConverter fun stringToSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
