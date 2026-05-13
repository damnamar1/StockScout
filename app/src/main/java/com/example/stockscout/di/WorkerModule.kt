package com.example.stockscout.di

import androidx.work.WorkerFactory
import com.example.stockscout.worker.SyncWorker
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import androidx.hilt.work.HiltWorkerFactory
import dagger.Binds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {
    // HiltWorkerFactory is provided automatically by the hilt-work artifact.
    // No explicit binding needed — Hilt generates it via @HiltWorker on SyncWorker.
}
