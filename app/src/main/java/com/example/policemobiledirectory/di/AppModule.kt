package com.example.policemobiledirectory.di

import android.content.Context
import com.example.policemobiledirectory.api.EmployeeApiService
import com.example.policemobiledirectory.data.local.EmployeeDao
import com.example.policemobiledirectory.data.local.PendingRegistrationDao
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.repository.ConstantsRepository
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.ImageRepository
import com.example.policemobiledirectory.repository.PendingRegistrationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton
import com.example.policemobiledirectory.di.IoDispatcher
import com.example.policemobiledirectory.utils.SecurityConfig

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSecurityConfig(
        @ApplicationContext context: Context
    ): SecurityConfig = SecurityConfig(context)

    @Provides
    @Singleton
    fun provideEmployeeRepository(
        auth: FirebaseAuth,
        employeeDao: EmployeeDao,
        firestore: FirebaseFirestore,
        apiService: EmployeeApiService,
        storage: FirebaseStorage,
        functions: FirebaseFunctions,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        securityConfig: SecurityConfig
    ): EmployeeRepository = EmployeeRepository(
        auth = auth,
        employeeDao = employeeDao,
        firestore = firestore,
        apiService = apiService,
        storage = storage,
        functions = functions,
        ioDispatcher = ioDispatcher,
        securityConfig = securityConfig
    )

    @Provides
    @Singleton
    fun providePendingRegistrationRepository(
        dao: PendingRegistrationDao,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        employeeRepository: EmployeeRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PendingRegistrationRepository = PendingRegistrationRepository(
        dao = dao,
        firestore = firestore,
        storage = storage,
        employeeRepository = employeeRepository,
        ioDispatcher = ioDispatcher
    )

    @Provides
    @Singleton
    fun provideConstantsRepository(
        @ApplicationContext context: Context,
        apiService: ConstantsApiService,
        securityConfig: SecurityConfig
    ): ConstantsRepository = ConstantsRepository(context, apiService, securityConfig)

    @Provides
    @Singleton
    fun provideImageRepository(
        @ApplicationContext context: Context
    ): ImageRepository = ImageRepository(context)

    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): SessionManager = SessionManager(context)
}
