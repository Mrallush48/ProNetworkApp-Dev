package com.pronetwork.app.di

import android.content.Context
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.BuildingDao
import com.pronetwork.app.data.PaymentDao
import com.pronetwork.app.data.PaymentTransactionDao
import com.pronetwork.app.data.SyncQueueDao
import com.pronetwork.app.network.AuthManager
import com.pronetwork.app.network.ConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Central Hilt module — provides all app-wide singletons.
 * Installed in SingletonComponent = lives as long as the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ========================
    // === Database Layer ===
    // ========================

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ClientDatabase {
        return ClientDatabase.getDatabase(context)
    }

    @Provides
    fun provideClientDao(db: ClientDatabase): ClientDao {
        return db.clientDao()
    }

    @Provides
    fun provideBuildingDao(db: ClientDatabase): BuildingDao {
        return db.buildingDao()
    }

    @Provides
    fun providePaymentDao(db: ClientDatabase): PaymentDao {
        return db.paymentDao()
    }

    @Provides
    fun providePaymentTransactionDao(db: ClientDatabase): PaymentTransactionDao {
        return db.paymentTransactionDao()
    }

    @Provides
    fun provideSyncQueueDao(db: ClientDatabase): SyncQueueDao {
        return db.syncQueueDao()
    }

    // ========================
    // === Auth Layer ===
    // ========================

    @Provides
    @Singleton
    fun provideAuthManager(@ApplicationContext context: Context): AuthManager {
        return AuthManager(context)
    }

    // ========================
    // === Network Layer ===
    // ========================

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return ConnectivityObserver(context)
    }

}
