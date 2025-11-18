package com.example.policemobiledirectory.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_prefs")

class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_ADMIN = booleanPreferencesKey("is_admin")
        val USER_NOTIF_LAST_SEEN = longPreferencesKey("user_notif_last_seen")
        val ADMIN_NOTIF_LAST_SEEN = longPreferencesKey("admin_notif_last_seen")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL] ?: "" }
    val isAdmin: Flow<Boolean> = context.dataStore.data.map { it[IS_ADMIN] ?: false }
    val userNotificationsSeenAt: Flow<Long> = context.dataStore.data.map { it[USER_NOTIF_LAST_SEEN] ?: 0L }
    val adminNotificationsSeenAt: Flow<Long> = context.dataStore.data.map { it[ADMIN_NOTIF_LAST_SEEN] ?: 0L }

    suspend fun saveLogin(email: String, isAdmin: Boolean) {
        context.dataStore.edit {
            it[IS_LOGGED_IN] = true
            it[USER_EMAIL] = email
            it[IS_ADMIN] = isAdmin
        }
    }

    suspend fun setUserNotificationsSeen(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[USER_NOTIF_LAST_SEEN] = timestamp
        }
    }

    suspend fun setAdminNotificationsSeen(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[ADMIN_NOTIF_LAST_SEEN] = timestamp
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.clear()
        }
    }
}
