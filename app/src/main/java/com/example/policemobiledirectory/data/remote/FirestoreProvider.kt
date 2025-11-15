package com.example.policemobiledirectory.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirestoreProvider {

    // ✅ Shared Firestore instance with offline persistence enabled
    val firestore: FirebaseFirestore by lazy {
        val instance = FirebaseFirestore.getInstance()
        instance.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        instance
    }
}
