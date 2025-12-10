package com.example.policemobiledirectory.repository

import android.util.Log
import com.example.policemobiledirectory.api.SyncApiService
import com.example.policemobiledirectory.api.OfficersSyncApiService
import com.google.gson.JsonParser
import com.example.policemobiledirectory.utils.SecurityConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import javax.inject.Inject

class SyncRepository @Inject constructor(
    private val api: SyncApiService,
    private val officersApi: OfficersSyncApiService,
    private val securityConfig: SecurityConfig
) {

    private fun token() = securityConfig.getSecretToken()

    suspend fun syncFirestoreToSheet(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            parseSyncMessage(api.syncFirebaseToSheet(token = token()), "Firestore → Sheet")
        }.onFailure {
            Log.e("SyncRepository", "syncFirestoreToSheet failed", it)
        }
    }

    suspend fun syncSheetToFirestore(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            parseSyncMessage(api.syncSheetToFirebase(token = token()), "Sheet → Firestore")
        }.onFailure {
            Log.e("SyncRepository", "syncSheetToFirestore failed", it)
        }
    }
    
    suspend fun syncOfficersSheetToFirestore(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            parseSyncMessage(officersApi.syncOfficersSheetToFirebase(token = token()), "Officers Sheet → Firestore")
        }.onFailure {
            Log.e("SyncRepository", "syncOfficersSheetToFirestore failed", it)
        }
    }

    private fun parseSyncMessage(body: ResponseBody, action: String): String {
        val raw = body.string().trim()
        if (raw.isBlank()) return "$action sync triggered"

        return try {
            val json = JsonParser.parseString(raw)
            when {
                json.isJsonObject -> {
                    val obj = json.asJsonObject
                    when {
                        obj.has("message") -> obj.get("message").asString
                        obj.has("error") -> "Sync failed: ${obj.get("error").asString}"
                        obj.has("success") && obj.get("success").asBoolean ->
                            "$action sync completed"
                        else -> raw
                    }
                }
                json.isJsonArray -> {
                    val size = json.asJsonArray.size()
                    val message = "$action sync started ($size entries)"
                    Log.d("SyncRepository", message)
                    message
                }
                else -> raw
            }
        } catch (e: Exception) {
            Log.w("SyncRepository", "Unable to parse sync response: $raw", e)
            raw
        }
    }
}

