package com.example.policemobiledirectory.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EmployeeEntity::class,
        PendingRegistrationEntity::class,
        AppIconEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun pendingRegistrationDao(): PendingRegistrationDao
    abstract fun appIconDao(): AppIconDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ✅ Migration 3 → 4: Add new column + new table
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

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "employee_directory_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5) // ✅ Keep user data on update
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
