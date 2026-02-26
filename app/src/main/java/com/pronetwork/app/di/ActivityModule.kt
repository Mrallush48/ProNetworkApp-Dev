package com.pronetwork.app.di

import android.app.Activity
import android.content.Context
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.export.ClientsExportManager
import com.pronetwork.app.export.ClientsImportManager
import com.pronetwork.app.export.DailyCollectionExportManager
import com.pronetwork.app.export.PaymentsExportManager
import com.pronetwork.app.repository.BuildingRepository
import com.pronetwork.app.repository.PaymentTransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

/**
 * Activity-scoped Hilt module — provides managers that require Activity context.
 * Installed in ActivityComponent = lives as long as the Activity.
 * Used for Export/Import managers that need ContentResolver, Intent, FileProvider, etc.
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    @ActivityScoped
    fun provideClientsExportManager(
        @ActivityContext context: Context
    ): ClientsExportManager {
        return ClientsExportManager(context)
    }

    @Provides
    @ActivityScoped
    fun providePaymentsExportManager(
        @ActivityContext context: Context,
        transactionRepo: PaymentTransactionRepository,
        db: ClientDatabase
    ): PaymentsExportManager {
        return PaymentsExportManager(context, transactionRepo, db)
    }

    @Provides
    @ActivityScoped
    fun provideDailyCollectionExportManager(
        @ActivityContext context: Context
    ): DailyCollectionExportManager {
        return DailyCollectionExportManager(context)
    }

    @Provides
    @ActivityScoped
    fun provideClientsImportManager(
        @ActivityContext context: Context,
        buildingRepo: BuildingRepository,
        db: ClientDatabase
    ): ClientsImportManager {
        return ClientsImportManager(context, buildingRepo, db)
    }
}
