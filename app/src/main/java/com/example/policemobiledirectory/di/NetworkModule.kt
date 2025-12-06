package com.example.policemobiledirectory.di

import com.example.policemobiledirectory.api.EmployeeApiService
import com.example.policemobiledirectory.api.SyncApiService
import com.example.policemobiledirectory.api.OfficersSyncApiService
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.data.remote.DocumentsApiService
import com.example.policemobiledirectory.data.remote.GalleryApiService
import com.example.policemobiledirectory.repository.DocumentsRepository
import com.example.policemobiledirectory.repository.GalleryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ✅ Documents API URL (for document uploads/management)
    private const val DOCUMENTS_BASE_URL =
        "https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/"

    // ✅ Gallery API URL (for gallery image uploads/management)
    private const val GALLERY_BASE_URL =
        "https://script.google.com/macros/s/AKfycbwXIhqfYWER3Z2KBlcrqZjyWCBfacHOeKCo_buWaZ6nG7qQpWaN91V7Y-IclzmOvG73/"

    // ✅ Employees sync URL (for employee Sheet ↔ Firestore sync)
    private const val EMPLOYEES_SYNC_BASE_URL =
       "https://script.google.com/macros/s/AKfycbyEqYeeUGeToFPwhdTD2xs7uEWOzlwIjYm1f41KJCWiQYL2Swipgg_y10xRekyV1s2fjQ/"
    // ✅ Officers sync URL (for officers Sheet → Firestore sync)
    private const val OFFICERS_SYNC_BASE_URL =
        "https://script.google.com/macros/s/AKfycbyYb-m0egcqz69JNbBYQj0Qv8qStnn6GlntPfK47Nj75bN7K3u2onqUaPgvAtPQjH8V/"
    
    // ✅ Constants API URL (for dynamic constants sync from Google Sheets)
    private const val CONSTANTS_BASE_URL =
        "https://script.google.com/macros/s/AKfycbyFMd7Qsv02wDYdM71ZCh_hUr08aFW6eYRztgmUYYI1ZuOKbKAXQtxnSZ3bhfbKWahY/"
    
    @Provides
    @Singleton
    @Named("OfficersSyncRetrofit")
    fun provideOfficersSyncRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(OFFICERS_SYNC_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    
    @Provides
    @Singleton
    fun provideOfficersSyncApiService(@Named("OfficersSyncRetrofit") retrofit: Retrofit): OfficersSyncApiService =
        retrofit.create(OfficersSyncApiService::class.java)

    @Provides
    @Singleton
    @Named("DocumentsRetrofit")
    fun provideDocumentsRetrofit(): Retrofit {
        // Extended timeouts for large document uploads
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)  // 3 minutes for large uploads
            .writeTimeout(180, TimeUnit.SECONDS)  // 3 minutes for large uploads
            .build()
        
        return Retrofit.Builder()
            .baseUrl(DOCUMENTS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("GalleryRetrofit")
    fun provideGalleryRetrofit(): Retrofit {
        // Extended timeouts for large image uploads
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)  // 3 minutes for large uploads
            .writeTimeout(180, TimeUnit.SECONDS)  // 3 minutes for large uploads
            .build()
        
        // ✅ Use lenient Gson to handle malformed JSON from Apps Script
        val gson = GsonBuilder()
            .setLenient()
            .create()
        
        return Retrofit.Builder()
            .baseUrl(GALLERY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("SyncRetrofit")
    fun provideSyncRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(EMPLOYEES_SYNC_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("ConstantsRetrofit")
    fun provideConstantsRetrofit(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(CONSTANTS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideEmployeeApiService(@Named("SyncRetrofit") retrofit: Retrofit): EmployeeApiService =
        retrofit.create(EmployeeApiService::class.java)

    // ✅ Documents API (separate from employee sync)
    @Provides
    @Singleton
    fun provideDocumentsApiService(@Named("DocumentsRetrofit") retrofit: Retrofit): DocumentsApiService =
        retrofit.create(DocumentsApiService::class.java)

    @Provides
    @Singleton
    fun provideSyncApiService(@Named("SyncRetrofit") syncRetrofit: Retrofit): SyncApiService =
        syncRetrofit.create(SyncApiService::class.java)

    // ✅ Gallery API (separate from documents)
    @Provides
    @Singleton
    fun provideGalleryApiService(@Named("GalleryRetrofit") retrofit: Retrofit): GalleryApiService =
        retrofit.create(GalleryApiService::class.java)

    // ✅ Constants API (for dynamic constants sync)
    @Provides
    @Singleton
    fun provideConstantsApiService(@Named("ConstantsRetrofit") retrofit: Retrofit): ConstantsApiService =
        retrofit.create(ConstantsApiService::class.java)

    // ✅ Add this if you didn't use @Inject constructor in repository
    @Provides
    @Singleton
    fun provideDocumentsRepository(api: DocumentsApiService): DocumentsRepository =
        DocumentsRepository(api)

    @Provides
    @Singleton
    fun provideGalleryRepository(api: GalleryApiService): GalleryRepository =
        GalleryRepository(api)
}
