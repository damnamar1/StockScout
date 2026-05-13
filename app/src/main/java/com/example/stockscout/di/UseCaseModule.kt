package com.example.stockscout.di

import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.domain.usecase.PickItemUseCase
import com.example.stockscout.domain.usecase.ResolveAliasUseCase
import com.example.stockscout.domain.usecase.SyncPendingPicksUseCase
import com.example.stockscout.utils.AliasResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides @Singleton
    fun provideAliasResolver(repository: ItemRepository) = AliasResolver(repository)

    @Provides @Singleton
    fun provideResolveAliasUseCase(repository: ItemRepository, resolver: AliasResolver) =
        ResolveAliasUseCase(repository, resolver)

    @Provides @Singleton
    fun providePickItemUseCase(repository: ItemRepository) = PickItemUseCase(repository)

    @Provides @Singleton
    fun provideSyncPendingPicksUseCase(repository: ItemRepository) =
        SyncPendingPicksUseCase(repository)
}
