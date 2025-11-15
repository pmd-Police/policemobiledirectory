package com.example.policemobiledirectory.data.local

import androidx.room.*

@Dao
interface AppIconDao {
    @Query("SELECT * FROM app_icons WHERE packageName = :pkg LIMIT 1")
    suspend fun getIcon(pkg: String): AppIconEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIcon(icon: AppIconEntity)

    @Query("DELETE FROM app_icons")
    suspend fun clearAll()
}
