package com.example.policemobiledirectory.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add email column to employees table
            db.execSQL("ALTER TABLE employees ADD COLUMN email TEXT")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // --- Employees table ---
            db.execSQL("ALTER TABLE employees ADD COLUMN department TEXT")
            db.execSQL("ALTER TABLE employees ADD COLUMN primaryMobileNumber TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE employees ADD COLUMN secondaryMobileNumber TEXT")

            // --- Pending Registrations table ---
            db.execSQL("ALTER TABLE pending_registrations ADD COLUMN primaryMobileNumber TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE pending_registrations ADD COLUMN secondaryMobileNumber TEXT")
        }
    }

    // Add future migrations here
}
