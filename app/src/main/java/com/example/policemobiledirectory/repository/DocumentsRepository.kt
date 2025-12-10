package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.data.remote.DocumentsApiService
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentsRepository @Inject constructor(
    private val api: DocumentsApiService,
    private val securityConfig: SecurityConfig
) {
    private fun token() = securityConfig.getSecretToken()
    private val gson = Gson()

    suspend fun fetchDocuments(): List<Document> {
        val response: Response<ResponseBody> = api.getDocumentsRaw(token = token())
        val bodyStr = response.body()?.string()
            ?: throw IllegalStateException("Empty documents response")

        // Try parse as array
        try {
            val listType = object : TypeToken<List<Document>>() {}.type
            return gson.fromJson(bodyStr, listType)
        } catch (_: Exception) {
            // try object with data/error
        }

        data class DocsApiResponse(
            val success: Boolean? = null,
            val data: List<Document>? = null,
            val error: String? = null,
            val message: String? = null
        )

        val obj = try {
            gson.fromJson(bodyStr, DocsApiResponse::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Unable to parse documents response: ${e.message}")
        }

        obj.data?.let { return it }

        val err = obj.error ?: obj.message ?: "Documents load failed"
        throw IllegalStateException(err)
    }

    suspend fun uploadDocument(request: DocumentUploadRequest) =
        api.uploadDocument(token = token(), request = request)

    suspend fun editDocument(request: DocumentEditRequest) =
        api.editDocument(token = token(), request = request)

    suspend fun deleteDocument(request: DocumentDeleteRequest) =
        api.deleteDocument(token = token(), request = request)
}
