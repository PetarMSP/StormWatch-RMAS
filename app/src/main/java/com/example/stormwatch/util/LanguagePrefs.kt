package com.example.stormwatch.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings_prefs")

class LanguagePrefs(private val context: Context) {

    private val KEY_IS_SERBIAN = booleanPreferencesKey("is_serbian")

    val isSerbianFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_IS_SERBIAN] ?: true } // default SR = true

    suspend fun setIsSerbian(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_SERBIAN] = value
        }
    }
}