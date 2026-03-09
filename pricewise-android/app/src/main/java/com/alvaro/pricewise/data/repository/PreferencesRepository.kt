package com.alvaro.pricewise.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pricewise_settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val CURRENCY_KEY = stringPreferencesKey("currency")
    }

    // ─── Tema ────────────────────────────────────────────

    fun isDarkTheme(): Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[DARK_THEME_KEY] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[DARK_THEME_KEY] = enabled
        }
    }

    // ─── Moneda ──────────────────────────────────────────

    fun getCurrency(): Flow<String> = context.settingsDataStore.data
        .map { prefs -> prefs[CURRENCY_KEY] ?: "EUR" }

    suspend fun setCurrency(currency: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[CURRENCY_KEY] = currency
        }
    }
}
