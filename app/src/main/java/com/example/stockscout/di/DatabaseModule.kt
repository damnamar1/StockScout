package com.example.stockscout.di

import android.content.Context
import androidx.room.Room
import com.example.stockscout.data.local.AppDatabase
import com.example.stockscout.data.local.dao.AliasDao
import com.example.stockscout.data.local.dao.ItemDao
import com.example.stockscout.data.local.dao.PendingPickDao
import com.example.stockscout.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, Constants.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideItemDao(db: AppDatabase): ItemDao = db.itemDao()
    @Provides fun provideAliasDao(db: AppDatabase): AliasDao = db.aliasDao()
    @Provides fun providePendingPickDao(db: AppDatabase): PendingPickDao = db.pendingPickDao()
}
