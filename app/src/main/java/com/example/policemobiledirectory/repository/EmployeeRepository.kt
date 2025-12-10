package com.example.policemobiledirectory.repository

import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.data.local.EmployeeEntity
import com.example.policemobiledirectory.data.local.EmployeeDao
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.utils.PinHasher
import com.example.policemobiledirectory.api.EmployeeApiService
import com.example.policemobiledirectory.utils.SecurityConfig

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.net.URL

import java.util.Date

// ------------------- REPOSITORY -------------------
open class EmployeeRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val employeeDao: EmployeeDao,
    private val firestore: FirebaseFirestore,
    private val apiService: EmployeeApiService,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val securityConfig: SecurityConfig
) {
    private val TAG = "EmployeeRepository"
    private val employeesCollection = firestore.collection("employees")
    private val storageRef = storage.reference
    private val metaDoc = firestore.collection("meta").document("idCounters")

    /**
     * Firestore rules require an authenticated user. If the app is logged out,
     * ensure we still have a lightweight anonymous Firebase session before
     * performing Firestore reads (e.g., login via email+PIN).
     */
    private suspend fun ensureFirestoreAuth() {
        if (auth.currentUser == null) {
            try {
                Log.d(TAG, "ensureFirestoreAuth: signing in anonymously for Firestore access")
                Firebase.auth.signInAnonymously().await()
            } catch (e: Exception) {
                Log.e(TAG, "ensureFirestoreAuth: anonymous sign-in failed", e)
                throw e
            }
        }
    }


    companion object {
        const val FIELD_EMAIL = "email"
        const val FIELD_FCM = "fcmToken"
        const val FIELD_KGID = "kgid"
        const val FIELD_PIN_HASH = "pin"              // pin hash stored in Firestore under "pin"
        const val RANK_PENDING = "Pending Verification"
    }

    // -------------------------------------------------------------------
    // SEARCH (Room backed)
    // -------------------------------------------------------------------
    fun searchEmployees(query: String, filter: SearchFilter): Flow<List<EmployeeEntity>> {
        val searchQuery = "%${query.trim().lowercase()}%"
        return when (filter) {
            SearchFilter.NAME -> employeeDao.searchByName(searchQuery)
            SearchFilter.KGID -> employeeDao.searchByKgid(searchQuery)
            SearchFilter.MOBILE -> employeeDao.searchByMobile(searchQuery)
            SearchFilter.STATION -> employeeDao.searchByStation(searchQuery)
            SearchFilter.METAL_NUMBER -> employeeDao.searchByMetalNumber(searchQuery)
            SearchFilter.RANK -> employeeDao.searchByRank(searchQuery)
        }
    }

    // -------------------------------------------------------------------
    // OFFLINE-FIRST LOGIN (Email + PIN hashed)
    // - 1) Try Room (offline)
    // - 2) If missing/mismatch, fallback to Firestore (online)
    // - 3) On successful online login, cache in Room
    // -------------------------------------------------------------------
    fun loginUserWithPin(email: String, pin: String): Flow<RepoResult<Employee>> = flow {
        emit(RepoResult.Loading)
        try {
            ensureFirestoreAuth()

            // Step 1 — Local lookup (Room)
            val localEmployee = employeeDao.getEmployeeByEmail(email)
            if (localEmployee != null) {
                val storedHash = localEmployee.pin
                if (!storedHash.isNullOrEmpty() && PinHasher.verifyPin(pin, storedHash)) {
                    Log.d(TAG, "LoginFlow: offline login success for $email")
                    emit(RepoResult.Success(localEmployee.toEmployee()))
                    return@flow
                }
            }

            // Step 2 — Firestore fallback
            val snapshot = employeesCollection
                .whereEqualTo(FIELD_EMAIL, email)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                emit(RepoResult.Error(null, "Invalid email or PIN"))
                return@flow
            }

            val doc = snapshot.documents.first()
            // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
            val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
            val remoteEmployee = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
            // read pin hash directly from document (defensive if Employee POJO doesn't contain it)
            val remotePinHash = doc.getString(FIELD_PIN_HASH).orEmpty()

            if (remotePinHash.isEmpty()) {
                emit(RepoResult.Error(null,"PIN not set for this account"))
                return@flow
            }

            // Step 3 — Verify entered PIN against stored hash (remote)
            if (!PinHasher.verifyPin(pin, remotePinHash)) {
                emit(RepoResult.Error(null,"Invalid email or PIN"))
                return@flow
            }

            try {
                FirebaseAuth.getInstance().signInAnonymously().await()
                Log.d(TAG, "🔥 Firebase anonymous login success (PIN login)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firebase anonymous login failed (PIN login)", e)
                emit(RepoResult.Error(e, "Login failed. Try again."))
                return@flow
            }

            // Step 4 — Cache remote record locally for offline use
            // Build entity from remote Employee object (or from doc fields)
            val entityToCache = buildEmployeeEntityFromDoc(doc, pinHash = remotePinHash)
            employeeDao.insertEmployee(entityToCache)
            emit(RepoResult.Success(entityToCache.toEmployee()))
        } catch (e: Exception) {
            Log.e(TAG, "loginUserWithPin failed", e)
            emit(RepoResult.Error(null,"Login failed: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // APPLY NEW PIN AFTER RESET (atomic: update Firestore then Room)
    // This is used when user has reset via email and now supplies new PIN in-app.
    // -------------------------------------------------------------------
    suspend fun applyNewPinAfterReset(email: String, newPinPlain: String): RepoResult<String> = withContext(ioDispatcher) {
        try {
            val newPinHash = PinHasher.hashPassword(newPinPlain)

            // 1) Find user document
            val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            if (snapshot.isEmpty) return@withContext RepoResult.Error(null,"No employee found for $email")

            val doc = snapshot.documents.first()
            val docRef = doc.reference

            // 2) Update Firestore
            val updates = mapOf(FIELD_PIN_HASH to newPinHash)
            docRef.update(updates).await()

            // 3) Update Room (local cache)
            val localEmp = employeeDao.getEmployeeByEmail(email)
            if (localEmp != null) {
                val updatedLocal = localEmp.copy(pin = newPinHash)
                employeeDao.insertEmployee(updatedLocal)
            } else {
                // If local missing, fetch the remote full object and insert
                // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
                val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                val updatedRemote = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
                if (updatedRemote != null) {
                    val entity = buildEmployeeEntityFromDoc(doc, pinHash = newPinHash)
                    employeeDao.insertEmployee(entity)
                }
            }

            Log.i(TAG, "applyNewPinAfterReset: PIN applied for $email")
            RepoResult.Success("New PIN applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "applyNewPinAfterReset failed", e)
            RepoResult.Error(null,"Failed to apply new PIN: ${e.message}")
        }
    }

    private suspend fun verifyEmailBeforePinChange(email: String): Boolean {
        // Example — adjust based on your actual verification system
        val document = firestore.collection("employees").document(email).get().await()
        return document.exists()
    }

    private suspend fun updatePinByEmail(email: String, hashedPin: String) {
        try {
            firestore.collection("employees")
                .document(email)
                .update("pin", hashedPin)
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to update PIN: ${e.message}")
        }
    }

    suspend fun deleteEmployee(kgid: String, photoUrl: String?) = withContext(ioDispatcher) {
        try {
            // ✅ 1️⃣ Delete from Google Sheet (via Apps Script API)
            apiService.deleteEmployee(
                token = securityConfig.getSecretToken(),
                kgid = kgid
            )

            // ✅ 2️⃣ Delete from Firestore
            firestore.collection("employees").document(kgid).delete().await()

            // ✅ 3️⃣ Delete from Room (local cache)
            employeeDao.deleteByKgid(kgid)

            // ✅ 4️⃣ Delete image from Google Drive (optional)
            photoUrl?.takeIf { it.contains("id=") }?.let { url ->
                val fileId = url.substringAfter("id=").substringBefore("&")
                deleteImageFromDrive(fileId)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun deleteImageFromDrive(fileId: String) {
        try {
            // ✅ FIX: Replaced khttp with a call to the Retrofit apiService
            val response = apiService.deleteImageFromDrive(
                token = securityConfig.getSecretToken(),
                fileId = fileId
            )
            if (response.isSuccessful) {
                Log.d(TAG, "deleteImageFromDrive: Successfully deleted image with fileId: $fileId")
            } else {
                Log.e(TAG, "deleteImageFromDrive: Failed to delete image. Server responded with code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteImageFromDrive: Network request failed", e)
        }
    }

    // -------------------------------------------------------------------
    // UNIFIED PIN UPDATE (update both Firestore and Room)
    // Use this when admin or user changes PIN from inside app (online)
    // -------------------------------------------------------------------
    suspend fun updateUserPinBothOnlineAndOffline(email: String, newPinPlain: String): RepoResult<String> = withContext(ioDispatcher) {
        try {
            val hashedPin = PinHasher.hashPassword(newPinPlain)

            // Update Firestore document (if exists)
            val querySnapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            if (querySnapshot.isEmpty) {
                return@withContext RepoResult.Error(null,"User not found in Firebase")
            }

            val docRef = querySnapshot.documents.first().reference
            docRef.update(FIELD_PIN_HASH, hashedPin).await()

            // Update Room cache
            val localEmp = employeeDao.getEmployeeByEmail(email)
            if (localEmp != null) {
                employeeDao.insertEmployee(localEmp.copy(pin = hashedPin))
            } else {
                // If local missing, insert remote doc
                val remoteDoc = querySnapshot.documents.first()
                val entity = buildEmployeeEntityFromDoc(remoteDoc, pinHash = hashedPin)
                employeeDao.insertEmployee(entity)
            }

            Log.i(TAG, "updateUserPinBothOnlineAndOffline: PIN updated for $email")
            RepoResult.Success("PIN updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "updateUserPinBothOnlineAndOffline failed", e)
            RepoResult.Error(null,"Failed to update PIN: ${e.message}")
        }
    }

    private fun generateRandomCode(length: Int = 6): String {
        val chars = "0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    suspend fun sendOtp(email: String): RepoResult<String> {
        return try {
            Log.d(TAG, "📤 Attempting to send OTP to: $email")

            val data = mapOf("email" to email)

            // CRITICAL FIX: explicitly asking for the 'asia-south1' instance
            val result = FirebaseFunctions.getInstance("asia-south1")
                .getHttpsCallable("requestOtp")
                .call(data)
                .await()

            // Safely extract the response map
            val responseData = result.data as? Map<*, *>
            Log.d(TAG, "✅ Cloud Function response: $responseData")

            if (responseData == null || responseData["success"] != true) {
                val msg = responseData?.get("message")?.toString() ?: "Unknown error from server."
                Log.e(TAG, "❌ Cloud Function returned error: $msg")
                return RepoResult.Error(Exception(msg), msg)
            }

            Log.d(TAG, "📩 OTP email successfully requested for $email")

            RepoResult.Success(responseData["message"]?.toString() ?: "Verification code sent to your email.")

        } catch (e: Exception) {
            Log.e(TAG, "💥 OTP send failed: ${e.message}", e)
            // If e.message contains "404", it specifically means the function URL was not found.
            RepoResult.Error(e, e.message ?: "Failed to send verification code.")
        }
    }


    // 🔹 3. Verify OTP code
    suspend fun verifyLoginCode(email: String, code: String): RepoResult<Employee> {
        val normalizedEmail = email.trim().lowercase()

        Log.d(TAG, "verifyLoginCode() called for normalizedEmail=$normalizedEmail code=$code")
        Log.e("EMAIL_CHECK", "Email entering verifyOtp = '${email}'")

        return try {
            val data = mapOf(
                "email" to normalizedEmail,
                "code" to code.trim()
            )

            val result = FirebaseFunctions
                .getInstance("asia-south1")
                .getHttpsCallable("verifyOtpEmail")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            Log.d(TAG, "✅ verifyOtpEmail response: $response")

            if (response == null || response["success"] != true) {
                val msg = response?.get("message")?.toString() ?: "OTP verification failed."
                Log.e(TAG, "❌ OTP verification failed: $msg")
                return RepoResult.Error(null, msg)
            }

            val empData = response["employee"] as? Map<*, *>
            if (empData == null) {
                Log.e(TAG, "❌ No employee data returned for $normalizedEmail")
                return RepoResult.Error(null, "No employee found for this email")
            }

            val emp = mapEmployeeFromResponse(empData)
            employeeDao.insertEmployee(emp.toEntity())

            try {
                FirebaseAuth.getInstance().signInAnonymously().await()
                Log.d(TAG, "🔥 Firebase anonymous login success (OTP flow)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firebase anonymous login failed (OTP flow)", e)
                return RepoResult.Error(e, "Login failed. Please try again.")
            }

            Log.d(TAG, "✅ Employee cached for $normalizedEmail")
            RepoResult.Success(emp)

        } catch (e: Exception) {
            Log.e(TAG, "💥 verifyLoginCode() failed", e)
            RepoResult.Error(e, "Failed to verify code: ${e.message}")
        }
    }


    // -------------------------------------------------------------------
    // FIRESTORE → ROOM SYNC (bulk)
    // -------------------------------------------------------------------
    suspend fun syncAllEmployees(): RepoResult<Unit> = withContext(ioDispatcher) {
        try {
            val snapshot = employeesCollection.get().await()
            // ✅ CRITICAL FIX: Ensure kgid is set from document ID for all employees
            val entities = snapshot.documents.mapNotNull { doc ->
                val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                val emp = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
                emp?.let { buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty()) }
            }
            employeeDao.insertEmployees(entities)
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "syncAllEmployees failed", e)
            RepoResult.Error(null,"Failed to sync employees: ${e.message}")
        }
    }

    // -------------------------------------------------------------------
    // CRUD and helpers
    // -------------------------------------------------------------------
    fun addOrUpdateEmployee(emp: Employee): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            val kgid = emp.kgid.takeIf { !it.isNullOrBlank() } ?: generateAutoId()
            // ✅ CRITICAL FIX: Ensure kgid is explicitly set in the Employee object
            val finalEmp = emp.copy(kgid = kgid)
            
            // ✅ FIX: Use merge to preserve existing fields that aren't being updated
            // This ensures fields like fcmToken, firebaseUid, etc. are not lost
            employeesCollection.document(kgid).set(finalEmp, SetOptions.merge()).await()
            
            // ✅ Log the save for debugging
            Log.d(TAG, "Saved employee: kgid=$kgid, metalNumber=${finalEmp.metalNumber}, name=${finalEmp.name}")
            
            // Insert/update into Room (no pin available here)
            employeeDao.insertEmployee(buildEmployeeEntityFromPojo(finalEmp))
            
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "addOrUpdateEmployee failed", e)
            emit(RepoResult.Error(null,"Failed to add/update employee: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    fun deleteEmployee(kgid: String): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            employeesCollection.document(kgid).delete().await()
            employeeDao.deleteByKgid(kgid)
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "deleteEmployee failed", e)
            emit(RepoResult.Error(null,"Failed to delete employee: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // Delete employee by email (helper function)
    suspend fun deleteEmployeeByEmail(email: String): RepoResult<Boolean> = withContext(ioDispatcher) {
        try {
            // Find employee by email
            val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            val doc = snapshot.documents.firstOrNull()
            
            if (doc == null) {
                return@withContext RepoResult.Error(null, "Employee with email $email not found")
            }
            
            // ✅ CRITICAL FIX: Use document ID as kgid if field is missing
            val kgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
            val employee = doc.toObject(Employee::class.java)?.copy(kgid = kgid)
            
            // Delete from Firestore
            employeesCollection.document(kgid).delete().await()
            
            // Delete from Room (local cache)
            employeeDao.deleteByKgid(kgid)
            
            // Delete from Google Sheet if API is available
            try {
                apiService.deleteEmployee(
                    token = securityConfig.getSecretToken(),
                    kgid = kgid
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete from Google Sheet: ${e.message}")
            }
            
            Log.i(TAG, "Successfully deleted employee: $kgid ($email)")
            RepoResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "deleteEmployeeByEmail failed", e)
            RepoResult.Error(null, "Failed to delete employee: ${e.message}")
        }
    }

    fun getEmployees(): Flow<RepoResult<List<Employee>>> = flow {
        emit(RepoResult.Loading)
        try {
            // getAllEmployees returns Flow<List<EmployeeEntity>> from DAO — use .first() to collect current
            var cached = employeeDao.getAllEmployees().first()
            if (cached.isEmpty()) {
                val snapshot = employeesCollection.get().await()
                // ✅ CRITICAL FIX: Ensure kgid is set from document ID for all employees
                val entities = snapshot.documents.mapNotNull { doc ->
                    val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                    val emp = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
                    emp?.let { buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty()) }
                }
                employeeDao.insertEmployees(entities)
                cached = employeeDao.getAllEmployees().first()
            }
            emit(RepoResult.Success(cached.map { it.toEmployee() }))
        } catch (e: Exception) {
            Log.e(TAG, "getEmployees failed", e)
            emit(RepoResult.Error(null,"Failed to fetch employees: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // PROFILE PHOTO UPLOAD
    // -------------------------------------------------------------------
    fun uploadProfilePhoto(uri: Uri, kgid: String): Flow<RepoResult<String>> = flow {
        emit(RepoResult.Loading)
        try {
            val fileRef = storageRef.child("profile_photos/$kgid.jpg")
            fileRef.putFile(uri).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            employeesCollection.document(kgid).update("photoUrl", downloadUrl).await()

            // Update local cache if present
            val local = employeeDao.getEmployeeByKgid(kgid)
            if (local != null) {
                employeeDao.insertEmployee(local.copy(photoUrl = downloadUrl))
            }

            emit(RepoResult.Success(downloadUrl))
        } catch (e: Exception) {
            Log.e(TAG, "uploadProfilePhoto failed", e)
            emit(RepoResult.Error(null,"Failed to upload photo: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // PENDING REGISTRATIONS
    // -------------------------------------------------------------------
    fun getPendingRegistrations(): Flow<List<PendingRegistrationEntity>> = flow {
        val snapshot = employeesCollection.whereEqualTo("rank", RANK_PENDING).get().await()
        val pending = snapshot.documents.mapNotNull { doc ->
            // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
            val docKgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
            val emp = doc.toObject(Employee::class.java)?.copy(kgid = docKgid)
            emp?.let {
                if (it.kgid.isNullOrBlank() || it.email.isNullOrBlank()) return@mapNotNull null
                PendingRegistrationEntity(
                    kgid = it.kgid,
                    name = it.name ?: "",
                    email = it.email ?: "",
                    mobile1 = it.mobile1 ?: "",
                    mobile2 = it.mobile2 ?: "",
                    station = it.station ?: "",
                    district = it.district ?: "",
                    bloodGroup = it.bloodGroup ?: "",
                    photoUrl = it.photoUrl ?: "",
                    firebaseUid = it.firebaseUid ?: "",
                    createdAt = it.createdAt ?: Date()
                )
            }
        }
        emit(pending)
    }.flowOn(ioDispatcher)

    fun approvePendingRegistration(entity: PendingRegistrationEntity): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            // ✅ CRITICAL FIX: Ensure kgid is set and not empty
            val kgid = entity.kgid.trim().takeIf { it.isNotBlank() } 
                ?: throw IllegalArgumentException("KGID is required for approval")
            val employee = entity.toEmployee().copy(
                rank = "Verified",
                kgid = kgid // ✅ Explicitly ensure kgid is set
            )
            val pendingQuery = employeesCollection.whereEqualTo("kgid", kgid).limit(1).get().await()
            val pendingDocId = pendingQuery.documents.firstOrNull()?.id
            // ✅ CRITICAL FIX: Use kgid as document ID and ensure kgid field is included
            val targetDocRef = employeesCollection.document(kgid)

            firestore.runBatch { batch ->
                // ✅ CRITICAL FIX: Ensure kgid field is explicitly included in document (already set in employee object)
                batch.set(targetDocRef, employee)
                pendingDocId?.let { id -> if (id != kgid) batch.delete(employeesCollection.document(id)) }
            }.await()

            employeeDao.insertEmployee(employee.toEntity())
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "approvePendingRegistration failed", e)
            emit(RepoResult.Error(null,"Approval failed: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    fun rejectPendingRegistration(entity: PendingRegistrationEntity, reason: String): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            val pendingDoc = employeesCollection.whereEqualTo("kgid", entity.kgid).limit(1).get().await().documents.firstOrNull()
            pendingDoc?.reference?.delete()?.await()
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "rejectPendingRegistration failed", e)
            emit(RepoResult.Error(null,"Rejection failed: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // GOOGLE SIGN-IN (only for registration / admin flows)
    // -------------------------------------------------------------------
    fun signInWithGoogleIdToken(idToken: String): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw IllegalStateException("Firebase user is null")
            val email = user.email ?: throw IllegalStateException("Google account has no email")
            val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            if (snapshot.isEmpty) emit(RepoResult.Error(null,"User not found: $email"))
            else emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogleIdToken failed", e)
            emit(RepoResult.Error(null,e.message ?: "Google Sign-In failed"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------
    private suspend fun generateAutoId(): String = withContext(ioDispatcher) {
        firestore.runTransaction { tx: Transaction ->
            val snapshot = tx.get(metaDoc)
            val current = snapshot.getLong("contactsCounter") ?: 0
            val next = current + 1
            tx.update(metaDoc, "contactsCounter", next)
            "AGID${next.toString().padStart(4, '0')}"
        }.await()
    }

    suspend fun getEmployeeByEmail(email: String): EmployeeEntity? = withContext(ioDispatcher) {
        // Primary: local Room
        val local = employeeDao.getEmployeeByEmail(email)
        if (local != null) return@withContext local

        // Fallback: Firestore
        val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
        val doc = snapshot.documents.firstOrNull()
        // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
        val docKgid = doc?.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc?.id ?: ""
        val remote = doc?.toObject(Employee::class.java)?.copy(kgid = docKgid)
        if (remote != null && doc != null) {
            val entity = buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty())
            employeeDao.insertEmployee(entity)
            return@withContext entity
        }
        return@withContext null
    }

    // Fetch current logged in user from Firebase and cache locally
    suspend fun getCurrentUserFromFirebase(): RepoResult<Employee?> = withContext(ioDispatcher) {
        try {
            val user = auth.currentUser ?: return@withContext RepoResult.Error(null,"No user logged in")
            val email = user.email ?: return@withContext RepoResult.Error(null,"No email found")
            val snapshot = employeesCollection.whereEqualTo(FIELD_EMAIL, email).limit(1).get().await()
            val doc = snapshot.documents.firstOrNull()
            // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
            val docKgid = doc?.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc?.id ?: ""
            val employee = doc?.toObject(Employee::class.java)?.copy(kgid = docKgid)
            if (doc != null && employee != null) {
                val entity = buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty())
                employeeDao.insertEmployee(entity)
            }
            RepoResult.Success(employee)
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUserFromFirebase failed", e)
            RepoResult.Error(null,"Failed to get user: ${e.message}")
        }
    }

    suspend fun logout() = withContext(ioDispatcher) {
        auth.signOut()
        employeeDao.clearEmployees()
    }

    // -------------------------------------------------------------------
    // ENTITY MAPPERS (Room <-> Domain)
    // -------------------------------------------------------------------
    private fun EmployeeEntity.toEmployee(): Employee {
        return Employee(
            kgid = kgid,
            name = name,
            email = email,
            pin = pin,
            mobile1 = mobile1,
            mobile2 = mobile2,
            rank = rank,
            metalNumber = metalNumber,
            district = district,
            station = station,
            bloodGroup = bloodGroup,
            photoUrl = photoUrl,
            photoUrlFromGoogle = photoUrlFromGoogle,
            fcmToken = fcmToken,
            firebaseUid = firebaseUid,
            isAdmin = isAdmin,
            isApproved = isApproved,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // convert domain Employee -> entity (pin not available from domain POJO usually)
    private fun Employee.toEntity(): EmployeeEntity {
        return EmployeeEntity(
            kgid = kgid,
            name = name,
            email = email,
            pin = pin,
            mobile1 = mobile1,
            mobile2 = mobile2,
            rank = rank,
            metalNumber = metalNumber,
            district = district,
            station = station,
            bloodGroup = bloodGroup,
            photoUrl = photoUrl,
            photoUrlFromGoogle = photoUrlFromGoogle,
            fcmToken = fcmToken,
            firebaseUid = firebaseUid,
            isAdmin = isAdmin,
            isApproved = isApproved,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // build EmployeeEntity from Firestore document (defensive)
    private fun buildEmployeeEntityFromDoc(doc: DocumentSnapshot, pinHash: String = ""): EmployeeEntity {
        // ✅ CRITICAL FIX: Always use document ID as kgid if field is missing
        val docKgid = doc.id // Document ID (e.g., "1953036")
        
        val emp = doc.toObject(Employee::class.java)
        return if (emp != null) {
            // Ensure kgid is set - use document ID if field is missing/empty
            val finalKgid = if (emp.kgid.isNotBlank()) emp.kgid else docKgid
            EmployeeEntity(
                kgid = finalKgid, // ✅ Always has a value (either from field or document ID)
                name = emp.name ?: "",
                email = emp.email ?: "",
                mobile1 = emp.mobile1 ?: "",
                mobile2 = emp.mobile2 ?: "",
                rank = emp.rank ?: "",
                metalNumber = emp.metalNumber ?: "",
                district = emp.district ?: "",
                station = emp.station ?: "",
                bloodGroup = emp.bloodGroup ?: "",
                photoUrl = emp.photoUrl ?: "",
                photoUrlFromGoogle = emp.photoUrlFromGoogle ?: "",
                fcmToken = emp.fcmToken ?: "",
                isAdmin = emp.isAdmin,
                isApproved = emp.isApproved ?: true, // Default to true if missing
                firebaseUid = emp.firebaseUid ?: "",
                createdAt = emp.createdAt ?: Date(),
                updatedAt = emp.updatedAt ?: Date(),
                pin = pinHash
            )
        } else {
            // If doc couldn't be mapped to Employee, build from fields directly
            // ✅ CRITICAL FIX: Use document ID as kgid if field is missing
            val fieldKgid = doc.getString("kgid")
            val finalKgid = if (!fieldKgid.isNullOrBlank()) fieldKgid else docKgid
            EmployeeEntity(
                kgid = finalKgid, // ✅ Always has a value (either from field or document ID)
                name = doc.getString("name") ?: "",
                email = doc.getString("email") ?: "",
                mobile1 = doc.getString("mobile1") ?: "",
                mobile2 = doc.getString("mobile2") ?: "",
                rank = doc.getString("rank") ?: "",
                metalNumber = doc.getString("metalNumber") ?: doc.getString("metal") ?: "", // Firestore field: "metalNumber" (fallback to "metal" for old data)
                district = doc.getString("district") ?: "",
                station = doc.getString("station") ?: "",
                bloodGroup = doc.getString("bloodGroup") ?: "",
                photoUrl = doc.getString("photoUrl") ?: "",
                photoUrlFromGoogle = doc.getString("photoUrlFromGoogle") ?: "",
                fcmToken = doc.getString("fcmToken") ?: "",
                isAdmin = doc.getBoolean("isAdmin") ?: false,
                isApproved = doc.getBoolean("isApproved") ?: true, // Default to true if missing
                firebaseUid = doc.getString("firebaseUid") ?: "",
                createdAt = doc.getDate("createdAt") ?: Date(),
                updatedAt = doc.getDate("updatedAt") ?: Date(),
                pin = pinHash
            )
        }
    }

    private fun mapEmployeeFromResponse(data: Map<*, *>): Employee {
        fun str(key: String): String = data[key]?.toString().orEmpty()
        fun bool(key: String): Boolean = when (val value = data[key]) {
            is Boolean -> value
            else -> value?.toString()?.toBoolean() ?: false
        }

        return Employee(
            kgid = str("kgid"),
            name = str("name"),
            email = str("email"),
            pin = str("pin").ifBlank { null },
            mobile1 = str("mobile1").ifBlank { null },
            mobile2 = str("mobile2").ifBlank { null },
            rank = str("rank").ifBlank { null },
            metalNumber = str("metalNumber").ifBlank { null },
            district = str("district").ifBlank { null },
            station = str("station").ifBlank { null },
            bloodGroup = str("bloodGroup").ifBlank { null },
            photoUrl = str("photoUrl").ifBlank { null },
            photoUrlFromGoogle = str("photoUrlFromGoogle").ifBlank { null },
            fcmToken = str("fcmToken").ifBlank { null },
            firebaseUid = str("firebaseUid").ifBlank { null },
            isAdmin = bool("isAdmin"),
            isApproved = data["isApproved"] as? Boolean ?: true
        )
    }

    // build EmployeeEntity from POJO (no pin)
    private fun buildEmployeeEntityFromPojo(emp: Employee, pinHash: String = ""): EmployeeEntity {
        return EmployeeEntity(
            kgid = emp.kgid,
            name = emp.name ?: "",
            email = emp.email ?: "",
            mobile1 = emp.mobile1 ?: "",
            mobile2 = emp.mobile2 ?: "",
            rank = emp.rank ?: "",
            metalNumber = emp.metalNumber ?: "",
            district = emp.district ?: "",
            station = emp.station ?: "",
            bloodGroup = emp.bloodGroup ?: "",
            photoUrl = emp.photoUrl ?: "",
            photoUrlFromGoogle = emp.photoUrlFromGoogle ?: "",
            fcmToken = emp.fcmToken ?: "",
            isAdmin = emp.isAdmin,
            isApproved = emp.isApproved,
            firebaseUid = emp.firebaseUid ?: "",
            createdAt = emp.createdAt ?: Date(),
            updatedAt = emp.updatedAt ?: Date(),
            pin = pinHash
        )
    }

    private fun PendingRegistrationEntity.toEmployee() = Employee(
        kgid = kgid,
        name = name,
        email = email,
        mobile1 = mobile1,
        mobile2 = mobile2,
        rank = rank ?: RANK_PENDING,
        station = station,
        district = district,
        bloodGroup = bloodGroup,
        photoUrl = photoUrl,
        fcmToken = "",
        isAdmin = false,
        firebaseUid = firebaseUid,
        createdAt = createdAt
    )

    // -------------------------
    // Additional helpers expected by ViewModel / calling code
    // -------------------------
    // For compatibility with viewmodel usage in your error list
    fun loginUser(email: String, pin: String): Flow<RepoResult<Employee>> = loginUserWithPin(email, pin)

    suspend fun updateUserPin(
        email: String,
        oldPin: String? = null,
        newPin: String,
        isForgot: Boolean = false
    ): RepoResult<String> {
        return try {
            Log.d("UpdatePinFlow", "Updating PIN for $email (isForgot=$isForgot)")

            // Step 1: Prepare the request payload
            val hashedNewPin = PinHasher.hashPassword(newPin)
            val data = mutableMapOf(
                "email" to email,
                "newPinHash" to hashedNewPin,
                "isForgot" to isForgot
            )

            if (!isForgot && oldPin != null) {
                val hashedOldPin = PinHasher.hashPassword(oldPin)
                data["oldPinHash"] = hashedOldPin
            }

            // Step 2: Call Firebase Cloud Function (✅ correct function name)
            val result = functions
                .getHttpsCallable("updateUserPin")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            Log.d("UpdatePinFlow", "✅ updateUserPin response: $response")

            if (response != null && response["success"] == true) {
                // Step 3: Update local Room cache
                employeeDao.updatePinByEmail(email, hashedNewPin)

                Log.d("UpdatePinFlow", "🎯 PIN successfully updated for $email via Cloud Function")
                RepoResult.Success(response["message"].toString())
            } else {
                val msg = response?.get("message")?.toString() ?: "Failed to update PIN."
                RepoResult.Error(null, msg)
            }

        } catch (e: Exception) {
            Log.e("UpdatePinFlow", "❌ Failed to update PIN for $email", e)
            RepoResult.Error(e, "Failed to update PIN: ${e.message}")
        }
    }

    suspend fun refreshEmployees() = withContext(ioDispatcher) {
        try {
            // Clear existing cache
            employeeDao.clearEmployees()
            
            // Force reload from Firestore
            val snapshot = employeesCollection.get().await()
            val entities = snapshot.documents.mapNotNull { doc ->
                try {
                    // ✅ CRITICAL FIX: Ensure kgid is set from document ID if field is missing
                    val kgid = doc.getString(FIELD_KGID)?.takeIf { it.isNotBlank() } ?: doc.id
                    val emp = doc.toObject(Employee::class.java)?.copy(kgid = kgid)
                    
                    // Skip deleted employees
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    if (isDeleted) {
                        Log.d(TAG, "Skipping deleted employee: $kgid")
                        return@mapNotNull null
                    }
                    
                    emp?.let { 
                        buildEmployeeEntityFromDoc(doc, pinHash = doc.getString(FIELD_PIN_HASH).orEmpty()) 
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing employee document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            if (entities.isNotEmpty()) {
                employeeDao.insertEmployees(entities)
                Log.d(TAG, "✅ Refreshed ${entities.size} employees from Firestore")
            } else {
                Log.w(TAG, "⚠️ No employees found in Firestore after refresh")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to refresh employees from Firestore", e)
        }
    }


    // helper wrapper to get user by email (returns RepoResult for consistency)
    suspend fun getUserByEmail(email: String): RepoResult<Employee?> = withContext(ioDispatcher) {
        try {
            val entity = getEmployeeByEmail(email) // <- This should return EmployeeEntity?
            if (entity != null) {
                RepoResult.Success(entity.toEmployee())  // ✅ Convert Entity -> Model safely
            } else {
                RepoResult.Success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserByEmail failed", e)
            RepoResult.Error(null, "Failed to get user: ${e.message}")
        }
    }

    // check if employee exists by (KGID + email) in Firestore (for forgot PIN, login, etc.)
    suspend fun checkEmployeeExists(kgid: String? = null, email: String? = null): Boolean = withContext(ioDispatcher) {
        try {
            when {
                !kgid.isNullOrBlank() -> {
                    val doc = employeesCollection.document(kgid).get().await()
                    doc.exists()
                }
                !email.isNullOrBlank() -> {
                    val snapshot = employeesCollection
                        .whereEqualTo(FIELD_EMAIL, email)
                        .limit(1)
                        .get()
                        .await()
                    snapshot.documents.isNotEmpty()
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkEmployeeExists failed", e)
            false
        }
    }


    // helper to get employee by kgid locally (used above)
    private suspend fun EmployeeDao.getEmployeeByKgidSafe(kgid: String): EmployeeEntity? {
        // Provide a convenience extension in this file if DAO lacks a direct method; attempts getEmployeeByKgid if exists.
        // If your DAO already has getEmployeeByKgid, this extension won't be used; otherwise you can add it to DAO.
        return try {
            // try direct function if present
            val method = this::class.java.declaredMethods.firstOrNull { it.name == "getEmployeeByKgid" }
            if (method != null) {
                @Suppress("UNCHECKED_CAST")
                method.invoke(this, kgid) as? EmployeeEntity
            } else null
        } catch (t: Throwable) {
            null
        }
    }

    // Provide a typed call for local lookup by kgid (calls DAO method if present)
    suspend fun getLocalEmployeeByKgid(kgid: String): EmployeeEntity? = withContext(ioDispatcher) {
        // If DAO provides a getEmployeeByKgid suspend function (recommended), it will be used directly.
        // We attempt to call it reflectively to avoid compilation errors if your DAO defines it.
        return@withContext try {
            // simple attempt: many DAOs implement a suspend fun getEmployeeByKgid(kgid: String): EmployeeEntity?
            val method = employeeDao::class.java.methods.firstOrNull { it.name == "getEmployeeByKgid" }
            if (method != null) {
                @Suppress("UNCHECKED_CAST")
                method.invoke(employeeDao, kgid) as? EmployeeEntity
            } else null
        } catch (t: Throwable) {
            null
        }
    }
    suspend fun getEmployeeDirect(email: String): Employee? = withContext(ioDispatcher) {
        val entity = getEmployeeByEmail(email)
        entity?.toEmployee()
    }
    
    /**
     * ✅ Insert EmployeeEntity directly into local DB
     * Used for refreshing current user after profile update
     */
    suspend fun insertEmployeeDirect(entity: EmployeeEntity) = withContext(ioDispatcher) {
        try {
            employeeDao.insertEmployee(entity)
            Log.d(TAG, "✅ Inserted employee entity: kgid=${entity.kgid}, metalNumber=${entity.metalNumber}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to insert employee entity: ${e.message}", e)
        }
    }

}
