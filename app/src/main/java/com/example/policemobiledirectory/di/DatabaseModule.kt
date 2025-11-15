package com.example.policemobiledirectory.di

import android.content.Context
import androidx.room.Room
import com.example.policemobiledirectory.data.local.AppDatabase
import com.example.policemobiledirectory.data.local.EmployeeDao
import com.example.policemobiledirectory.data.local.PendingRegistrationDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "police_directory.db"
        )
            // Optional fallback (use only during dev):
            // .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideEmployeeDao(database: AppDatabase): EmployeeDao =
        database.employeeDao()

    @Provides
    @Singleton
    fun providePendingRegistrationDao(database: AppDatabase): PendingRegistrationDao =
        database.pendingRegistrationDao()
}
