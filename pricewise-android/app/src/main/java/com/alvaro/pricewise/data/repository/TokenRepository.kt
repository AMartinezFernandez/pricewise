package com.alvaro.pricewise.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pricewise_prefs")

@Singleton
class TokenRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val ROLE_KEY = stringPreferencesKey("role")
        private val COMPANY_ID_KEY = longPreferencesKey("company_id")
        private val COMPANY_NAME_KEY = stringPreferencesKey("company_name")
    }

    @Volatile
    private var cachedToken: String? = null

    /** Señal para que el interceptor espere a la hidratación inicial del token. */
    private val hydrationLatch = CountDownLatch(1)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            cachedToken = context.dataStore.data.first()[TOKEN_KEY]
            hydrationLatch.countDown()
        }
    }

    fun getToken(): Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[TOKEN_KEY]?.also { cachedToken = it } }

    /**
     * Devuelve el token cacheado de forma síncrona.
     * Espera hasta 2s a la hidratación inicial si aún no terminó.
     * Seguro para usar en interceptors de OkHttp (corren en thread pool, no en main).
     */
    fun getCachedToken(): String? {
        hydrationLatch.await(2, TimeUnit.SECONDS)
        return cachedToken
    }

    fun getUserId(): Flow<Long?> = context.dataStore.data
        .map { prefs -> prefs[USER_ID_KEY] }

    fun getUsername(): Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[USERNAME_KEY] }

    fun getRole(): Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[ROLE_KEY] }

    fun getCompanyId(): Flow<Long?> = context.dataStore.data
        .map { prefs -> prefs[COMPANY_ID_KEY] }

    fun getCompanyName(): Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[COMPANY_NAME_KEY] }

    fun isLoggedIn(): Flow<Boolean> = context.dataStore.data
        .map { prefs -> !prefs[TOKEN_KEY].isNullOrBlank() }

    suspend fun saveSession(
        token: String, userId: Long, username: String, role: String,
        companyId: Long? = null, companyName: String? = null
    ) {
        cachedToken = token
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
            prefs[USERNAME_KEY] = username
            prefs[ROLE_KEY] = role
            companyId?.let { prefs[COMPANY_ID_KEY] = it }
            companyName?.let { prefs[COMPANY_NAME_KEY] = it }
        }
    }

    suspend fun clearSession() {
        cachedToken = null
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USERNAME_KEY)
            prefs.remove(ROLE_KEY)
            prefs.remove(COMPANY_ID_KEY)
            prefs.remove(COMPANY_NAME_KEY)
        }
    }
}
