package com.example.policemobiledirectory.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    // ✅ Migration 3 → 4: Add isApproved column + new table
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // Add new column to Employee table if missing
                database.execSQL(
                    "ALTER TABLE employees ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 1"
                )
            } catch (e: Exception) {
                // Column might already exist, ignore
            }

            // Create new table for AppIconEntity if not exists
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_icons (
                    packageName TEXT PRIMARY KEY NOT NULL,
                    iconUrl TEXT NOT NULL,
                    lastUpdated INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    // ✅ Migration 4 → 5: Add updatedAt column to employees table
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // Add updatedAt column to Employee table
                database.execSQL(
                    "ALTER TABLE employees ADD COLUMN updatedAt INTEGER"
                )
            } catch (e: Exception) {
                // Column might already exist, ignore
            }
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "employee_directory_db"
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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
