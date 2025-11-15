package com.example.policemobiledirectory.di

import com.example.policemobiledirectory.api.EmployeeApiService
import com.example.policemobiledirectory.data.remote.DocumentsApiService
import com.example.policemobiledirectory.repository.DocumentsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ✅ Base URL (DO NOT include /exec at the end)
    private const val BASE_URL =
        "https://script.google.com/macros/s/AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/"

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideEmployeeApiService(retrofit: Retrofit): EmployeeApiService =
        retrofit.create(EmployeeApiService::class.java)

    // ✅ Add this new provider for Documents API
    @Provides
    @Singleton
    fun provideDocumentsApiService(retrofit: Retrofit): DocumentsApiService =
        retrofit.create(DocumentsApiService::class.java)

    // ✅ Add this if you didn't use @Inject constructor in repository
    @Provides
    @Singleton
    fun provideDocumentsRepository(api: DocumentsApiService): DocumentsRepository =
        DocumentsRepository(api)
}
