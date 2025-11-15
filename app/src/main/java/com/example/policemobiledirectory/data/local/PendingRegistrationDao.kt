package com.example.policemobiledirectory.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingRegistrationDao {

    /* -----------------------------------------------------
       INSERTS
    ----------------------------------------------------- */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingRegistrationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PendingRegistrationEntity>)

    /* -----------------------------------------------------
       DELETE
    ----------------------------------------------------- */

    @Delete
    suspend fun delete(entity: PendingRegistrationEntity)

    @Query("DELETE FROM pending_registrations")
    suspend fun deleteAll()

    @Query("DELETE FROM pending_registrations WHERE roomId = :roomId")
    suspend fun deleteById(roomId: Long)

    /* -----------------------------------------------------
       UPDATES
    ----------------------------------------------------- */

    @Update
    suspend fun update(entity: PendingRegistrationEntity)

    @Query("""
        UPDATE pending_registrations 
        SET status = 'approved', 
            isApproved = 1, 
            updatedAt = CURRENT_TIMESTAMP 
        WHERE roomId = :roomId
    """)
    suspend fun approve(roomId: Long)

    @Query("""
        UPDATE pending_registrations 
        SET status = 'rejected', 
            rejectionReason = :reason, 
            updatedAt = CURRENT_TIMESTAMP 
        WHERE roomId = :roomId
    """)
    suspend fun reject(roomId: Long, reason: String)

    /* -----------------------------------------------------
       QUERIES
    ----------------------------------------------------- */

    @Query("SELECT * FROM pending_registrations WHERE status = 'pending' ORDER BY createdAt DESC")
    fun getAllPending(): Flow<List<PendingRegistrationEntity>>

    @Query("SELECT * FROM pending_registrations WHERE kgid = :kgid LIMIT 1")
    suspend fun getByKgid(kgid: String): PendingRegistrationEntity?

    @Query("SELECT * FROM pending_registrations WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun findByFirestoreId(firestoreId: String?): PendingRegistrationEntity?
}
