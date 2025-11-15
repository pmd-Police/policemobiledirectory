package com.example.policemobiledirectory.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "PoliceMobileDirectoryPrefs"
        private const val KEY_KGID = "kgid"
        private const val KEY_PASSWORD = "password" // Security Warning: Storing raw passwords is not secure for production.
        private const val KEY_REMEMBER_ME = "remember_me"
    }

    fun saveLoginPreference(kgid: String, passwordToSave: String, rememberMe: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe) {
                putString(KEY_KGID, kgid)
                putString(KEY_PASSWORD, passwordToSave)
            } else {
                // If rememberMe is false, clear previously saved credentials
                remove(KEY_KGID)
                remove(KEY_PASSWORD)
            }
            apply()
        }
    }

    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun getKgid(): String? {
        return if (isRememberMeEnabled()) {
            prefs.getString(KEY_KGID, null)
        } else {
            null
        }
    }

    fun getPassword(): String? {
        return if (isRememberMeEnabled()) {
            prefs.getString(KEY_PASSWORD, null)
        } else {
            null
        }
    }

    fun clearLoginDetails() {
        prefs.edit().apply {
            remove(KEY_KGID)
            remove(KEY_PASSWORD)
            remove(KEY_REMEMBER_ME) // Also clear the remember me flag itself
            apply()
        }
    }
}
