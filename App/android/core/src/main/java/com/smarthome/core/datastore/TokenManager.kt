package com.smarthome.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

interface TokenManager {
    fun getAccessToken(): Flow<String?>
    fun getFcmToken(): Flow<String?>
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun saveFcmToken(fcmToken: String)
    suspend fun clearTokens()
}

@Singleton
class DataStoreTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenManager {

    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val FCM_TOKEN = stringPreferencesKey("fcm_token")

    override fun getAccessToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN]
        }
    }

    override fun getFcmToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[FCM_TOKEN]
        }
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }

    override suspend fun saveFcmToken(fcmToken: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN] = fcmToken
        }
    }

    override suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
        }
    }
}
