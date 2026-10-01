package com.sumi.jamplay.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlayerPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val SHUFFLE_KEY = booleanPreferencesKey("shuffle_mode")
    private val REPEAT_KEY = intPreferencesKey("repeat_mode")

    val shuffleMode: Flow<Boolean> = context.playerDataStore.data
        .map { prefs -> prefs[SHUFFLE_KEY] ?: false }

    val repeatMode: Flow<Int> = context.playerDataStore.data
        .map { prefs -> prefs[REPEAT_KEY] ?: 0 }

    suspend fun setShuffleMode(mode: Boolean) {
        context.playerDataStore.edit { prefs ->
            prefs[SHUFFLE_KEY] = mode
        }
    }

    suspend fun setRepeatMode(mode: Int) {
        context.playerDataStore.edit { prefs ->
            prefs[REPEAT_KEY] = mode
        }
    }
}