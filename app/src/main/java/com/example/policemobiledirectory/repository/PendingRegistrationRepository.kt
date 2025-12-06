package com.example.policemobiledirectory.repository

import android.net.Uri
import com.example.policemobiledirectory.data.local.PendingRegistrationDao
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.toEmployee
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingRegistrationRepository @Inject constructor(
    private val dao: PendingRegistrationDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val employeeRepository: EmployeeRepository,
    private val ioDispatcher: CoroutineDispatcher
) {

    /* ----------------------------------------------------------
        FETCH PENDING (Firestore → Room)
    ----------------------------------------------------------- */
    suspend fun fetchPendingFromFirestore(): RepoResult<List<PendingRegistrationEntity>> {
        return try {
            val snapshot = firestore.collection("pending_registrations")
                .whereEqualTo("status", "pending")
                .get().await()

            val list = snapshot.toObjects(PendingRegistrationEntity::class.java)
            RepoResult.Success(list)
        } catch (e: Exception) {
            RepoResult.Error(e, "Failed to load pending registrations: ${e.message}")
        }
    }

    suspend fun saveAllToLocal(list: List<PendingRegistrationEntity>) =
        withContext(ioDispatcher) { dao.insertAll(list) }

    fun getLocalPending(): Flow<List<PendingRegistrationEntity>> =
        dao.getAllPending()


    /* ----------------------------------------------------------
        SUBMIT NEW REGISTRATION
    ----------------------------------------------------------- */
    suspend fun addPendingRegistration(entity: PendingRegistrationEntity): Flow<RepoResult<String>> =
        flow {
            emit(RepoResult.Loading)

            try {
                val docRef = firestore.collection("pending_registrations").document()

                val prepared = entity.copy(
                    firestoreId = docRef.id,
                    status = "pending",
                    createdAt = java.util.Date(),
                    isApproved = false
                )

                // Firestore save
                docRef.set(prepared).await()

                // Local save
                dao.insert(prepared)

                emit(RepoResult.Success("Registration submitted for approval"))
            } catch (e: Exception) {
                emit(RepoResult.Error(e, "Failed: ${e.message}"))
            }
        }

    suspend fun updatePendingRegistration(entity: PendingRegistrationEntity): RepoResult<String> {
        return try {
            val docId = entity.firestoreId ?: return RepoResult.Error(message = "Missing document id")

            firestore.collection("pending_registrations")
                .document(docId)
                .set(entity, SetOptions.merge())
                .await()

            withContext(ioDispatcher) { dao.update(entity) }

            RepoResult.Success("Pending registration updated.")
        } catch (e: Exception) {
            RepoResult.Error(e, "Update failed: ${e.message}")
        }
    }


    /* ----------------------------------------------------------
        UPLOAD PHOTO FOR APPROVAL
    ----------------------------------------------------------- */
    suspend fun uploadPhoto(entity: PendingRegistrationEntity, uri: Uri): String {
        val ref = storage.reference.child(
            "pending_photos/${entity.kgid}_${System.currentTimeMillis()}.jpg"
        )

        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }


    /* ----------------------------------------------------------
        APPROVE REGISTRATION → CREATE EMPLOYEE
    ----------------------------------------------------------- */
    suspend fun approve(entity: PendingRegistrationEntity): RepoResult<String> {
        return try {
            // ✅ CRITICAL FIX: Validate kgid is not empty before approval
            val kgid = entity.kgid.trim().takeIf { it.isNotBlank() }
                ?: return RepoResult.Error(message = "KGID is required for approval")
            
            val photoUrl = entity.photoUrl ?: entity.photoUrlFromGoogle

            // ✅ CRITICAL FIX: Ensure kgid is explicitly set in Employee object
            val employee = entity.toEmployee(photoUrl).copy(kgid = kgid)

            // 1️⃣ Save employee (Google Sheet + Room)
            val result = employeeRepository.addOrUpdateEmployee(employee).last()
            if (result !is RepoResult.Success)
                return RepoResult.Error(message = "Failed to save employee")

            // 2️⃣ Firestore update
            firestore.collection("pending_registrations")
                .document(entity.firestoreId!!)
                .update(
                    mapOf(
                        "status" to "approved",
                        "isApproved" to true,
                        "updatedAt" to java.util.Date()
                    )
                ).await()

            // 3️⃣ Local DB update
            entity.roomId?.let { dao.approve(it) }

            RepoResult.Success("Approved successfully")
        } catch (e: Exception) {
            RepoResult.Error(e, "Approval failed: ${e.message}")
        }
    }


    /* ----------------------------------------------------------
        REJECT REGISTRATION
    ----------------------------------------------------------- */
    suspend fun reject(entity: PendingRegistrationEntity, reason: String): RepoResult<String> {
        return try {
            firestore.collection("pending_registrations")
                .document(entity.firestoreId!!)
                .update(
                    mapOf(
                        "status" to "rejected",
                        "rejectionReason" to reason,
                        "updatedAt" to java.util.Date()
                    )
                ).await()

            entity.roomId?.let { dao.reject(it, reason) }

            RepoResult.Success("Registration rejected")
        } catch (e: Exception) {
            RepoResult.Error(e, "Rejection failed: ${e.message}")
        }
    }
}
